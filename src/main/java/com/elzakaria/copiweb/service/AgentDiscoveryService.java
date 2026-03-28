package com.elzakaria.copiweb.service;

import com.elzakaria.copiweb.dto.AgentCatalogView;
import com.elzakaria.copiweb.dto.DiscoveredAgentView;
import com.elzakaria.copiweb.dto.InstalledPluginView;
import com.elzakaria.copiweb.dto.RegisteredMarketplaceView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentDiscoveryService {

    private static final String FALLBACK_MODEL = "gpt-4.1";
    private static final Pattern LEADING_SYMBOLS = Pattern.compile("^[^\\p{Alnum}_-]+");

    private final CopilotClient copilotClient;
    private final ModelService modelService;
    private final List<ToolDefinition> toolDefinitions;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentCatalogView getCatalog() {
        return new AgentCatalogView(
                listAgents(),
                listInstalledPlugins(),
                listMarketplaces()
        );
    }

    public List<DiscoveredAgentView> listAgents() {
        CopilotSession probeSession = null;
        String probeSessionId = null;

        try {
            var config = new SessionConfig()
                    .setModel(resolveDiscoveryModel())
                    .setStreaming(false)
                    .setTools(this.toolDefinitions)
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

            probeSession = copilotClient.createSession(config).get(30, TimeUnit.SECONDS);
            probeSessionId = probeSession.getSessionId();

            var agents = probeSession.listAgents().get(30, TimeUnit.SECONDS);
            String selectedAgentName = null;
            try {
                var currentAgent = probeSession.getCurrentAgent().get(10, TimeUnit.SECONDS);
                selectedAgentName = currentAgent != null ? currentAgent.getName() : null;
            } catch (Exception e) {
                log.debug("No current custom agent selected in discovery session: {}", e.getMessage());
            }
            final String currentSelectedAgentName = selectedAgentName;

            return agents.stream()
                    .map(agent -> new DiscoveredAgentView(
                            agent.getName(),
                            defaultString(agent.getDisplayName(), agent.getName()),
                            defaultString(agent.getDescription(), "Detected from the local Copilot runtime."),
                            agent.getName() != null && agent.getName().equals(currentSelectedAgentName)
                    ))
                    .sorted(Comparator
                            .comparing(DiscoveredAgentView::selected).reversed()
                            .thenComparing(DiscoveredAgentView::displayName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to discover custom agents from Copilot runtime: {}", e.getMessage());
            return List.of();
        } finally {
            if (probeSession != null) {
                try {
                    probeSession.close();
                } catch (Exception e) {
                    log.debug("Failed to close discovery session {}: {}", probeSessionId, e.getMessage());
                }
            }
            if (probeSessionId != null) {
                try {
                    copilotClient.deleteSession(probeSessionId).get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.debug("Failed to delete discovery session {}: {}", probeSessionId, e.getMessage());
                }
            }
        }
    }

    public List<InstalledPluginView> listInstalledPlugins() {
        Path pluginsRoot = getInstalledPluginsRoot();
        if (!Files.isDirectory(pluginsRoot)) {
            return List.of();
        }

        try (Stream<Path> manifests = Files.walk(pluginsRoot, 4)) {
            return manifests
                    .filter(path -> path.getFileName().toString().equals("plugin.json"))
                    .map(path -> readInstalledPlugin(path, pluginsRoot))
                    .flatMap(Optional::stream)
                    .sorted(Comparator
                            .comparing(InstalledPluginView::source, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(InstalledPluginView::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException e) {
            log.warn("Failed to inspect installed Copilot plugins under {}: {}", pluginsRoot, e.getMessage());
            return List.of();
        }
    }

    Optional<InstalledPluginView> readInstalledPlugin(Path manifestPath, Path pluginsRoot) {
        try {
            JsonNode manifest = objectMapper.readTree(Files.readString(manifestPath));
            Path pluginRoot = manifestPath.getParent();
            String declaredAgentsPath = textValue(manifest.get("agents"));

            boolean declaresAgents = declaredAgentsPath != null;
            boolean hasAgentFiles = declaresAgents && hasAgentFiles(pluginRoot, declaredAgentsPath);

            return Optional.of(new InstalledPluginView(
                    defaultString(textValue(manifest.get("name")), pluginRoot.getFileName().toString()),
                    defaultString(textValue(manifest.get("description")), "Installed Copilot plugin."),
                    defaultString(textValue(manifest.get("version")), "unknown"),
                    determinePluginSource(manifestPath, pluginsRoot),
                    declaresAgents,
                    hasAgentFiles,
                    hasNonBlankValue(manifest.get("skills")),
                    hasNonBlankValue(manifest.get("hooks")),
                    hasNonBlankValue(manifest.get("mcpServers"))
            ));
        } catch (IOException e) {
            log.warn("Failed to read plugin manifest {}: {}", manifestPath, e.getMessage());
            return Optional.empty();
        }
    }

    public List<RegisteredMarketplaceView> listMarketplaces() {
        try {
            String output = runCopilotCommand("plugin", "marketplace", "list");
            return parseMarketplaces(output);
        } catch (Exception e) {
            log.warn("Failed to inspect Copilot plugin marketplaces: {}", e.getMessage());
            return List.of();
        }
    }

    List<RegisteredMarketplaceView> parseMarketplaces(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }

        List<RegisteredMarketplaceView> marketplaces = new ArrayList<>();
        Boolean builtInSection = null;

        for (String rawLine : output.lines().toList()) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            if (line.contains("Included with GitHub Copilot")) {
                builtInSection = true;
                continue;
            }
            if (line.startsWith("Registered marketplaces:")) {
                builtInSection = false;
                continue;
            }
            if (!line.contains("(GitHub:") || builtInSection == null) {
                continue;
            }

            int repoStart = line.indexOf("(GitHub:");
            int repoEnd = line.lastIndexOf(')');
            if (repoStart < 0 || repoEnd <= repoStart) {
                continue;
            }

            String name = sanitizeLeadingSymbols(line.substring(0, repoStart).trim());
            String repository = line.substring(repoStart + "(GitHub:".length(), repoEnd).trim();

            if (!name.isBlank() && !repository.isBlank()) {
                marketplaces.add(new RegisteredMarketplaceView(name, repository, builtInSection));
            }
        }

        return marketplaces.stream()
                .sorted(Comparator
                        .comparing(RegisteredMarketplaceView::builtIn).reversed()
                        .thenComparing(RegisteredMarketplaceView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private String resolveDiscoveryModel() {
        return modelService.getModels().stream()
                .map(model -> model.getId())
                .filter(modelId -> modelId != null && !modelId.isBlank())
                .findFirst()
                .orElse(FALLBACK_MODEL);
    }

    private Path getInstalledPluginsRoot() {
        return Path.of(System.getProperty("user.home"), ".copilot", "installed-plugins");
    }

    private boolean hasAgentFiles(Path pluginRoot, String declaredAgentsPath) {
        Path candidatePath = pluginRoot.resolve(declaredAgentsPath).normalize();
        if (!candidatePath.startsWith(pluginRoot.normalize()) || !Files.isDirectory(candidatePath)) {
            return false;
        }

        try (Stream<Path> files = Files.walk(candidatePath, 2)) {
            return files.anyMatch(Files::isRegularFile);
        } catch (IOException e) {
            log.debug("Failed to inspect agent files under {}: {}", candidatePath, e.getMessage());
            return false;
        }
    }

    private boolean hasNonBlankValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return false;
        }
        if (node.isTextual()) {
            return !node.asText().isBlank();
        }
        if (node.isArray()) {
            return StreamSupportHelper.stream(node).anyMatch(this::hasNonBlankValue);
        }
        if (node.isObject()) {
            return node.fieldNames().hasNext();
        }
        return true;
    }

    private String determinePluginSource(Path manifestPath, Path pluginsRoot) {
        Path relativePath = pluginsRoot.relativize(manifestPath);
        return relativePath.getNameCount() > 0 ? relativePath.getName(0).toString() : "local";
    }

    private String runCopilotCommand(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("copilot");
        command.addAll(List.of(args));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String output;
        try (var input = process.getInputStream()) {
            output = new String(input.readAllBytes(), Charset.defaultCharset());
        }

        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("copilot command timed out: " + String.join(" ", command));
        }

        if (process.exitValue() != 0) {
            throw new IOException("copilot command failed with exit code " + process.exitValue());
        }

        return output;
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String value = node.asText();
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private String defaultString(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private String sanitizeLeadingSymbols(String value) {
        return LEADING_SYMBOLS.matcher(value).replaceFirst("").trim();
    }

    private static final class StreamSupportHelper {
        private StreamSupportHelper() {
        }

        static Stream<JsonNode> stream(JsonNode arrayNode) {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(arrayNode.elements(), 0), false);
        }
    }
}
