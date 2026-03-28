package com.elzakaria.copiweb.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AgentDiscoveryServiceTest {

    private final AgentDiscoveryService service = new AgentDiscoveryService(null, null, null);

    @Test
    void parseMarketplacesParsesBuiltInAndRegisteredSources() {
        String output = """
                ✨ Included with GitHub Copilot:
                  ◆ copilot-plugins (GitHub: github/copilot-plugins)
                  ◆ awesome-copilot (GitHub: github/awesome-copilot)
                Registered marketplaces:
                  • understand-anything (GitHub: Lum1104/Understand-Anything)
                """;

        var marketplaces = service.parseMarketplaces(output);

        assertThat(marketplaces).hasSize(3);
        assertThat(marketplaces)
                .extracting(marketplace -> marketplace.name() + ":" + marketplace.repository() + ":" + marketplace.builtIn())
                .containsExactly(
                        "awesome-copilot:github/awesome-copilot:true",
                        "copilot-plugins:github/copilot-plugins:true",
                        "understand-anything:Lum1104/Understand-Anything:false"
                );
    }

    @Test
    void readInstalledPluginExtractsPluginCapabilities(@TempDir Path tempDir) throws Exception {
        final var pluginsRoot = tempDir.resolve("installed-plugins");
        final var pluginRoot = pluginsRoot.resolve("_direct").resolve("demo-plugin");
        Files.createDirectories(pluginRoot.resolve("agents"));
        Files.writeString(pluginRoot.resolve("agents").resolve("hello-agent.md"), "# hello");
        Files.writeString(pluginRoot.resolve("plugin.json"), """
                {
                  "name": "demo-plugin",
                  "description": "Demo plugin",
                  "version": "1.2.3",
                  "agents": "agents/",
                  "skills": ["skills/"],
                  "hooks": "hooks.json",
                  "mcpServers": ".mcp.json"
                }
                """);

        var plugin = service.readInstalledPlugin(pluginRoot.resolve("plugin.json"), pluginsRoot).orElseThrow();

        assertThat(plugin.name()).isEqualTo("demo-plugin");
        assertThat(plugin.description()).isEqualTo("Demo plugin");
        assertThat(plugin.version()).isEqualTo("1.2.3");
        assertThat(plugin.source()).isEqualTo("_direct");
        assertThat(plugin.declaresAgents()).isTrue();
        assertThat(plugin.hasAgentFiles()).isTrue();
        assertThat(plugin.hasSkills()).isTrue();
        assertThat(plugin.hasHooks()).isTrue();
        assertThat(plugin.hasMcpServers()).isTrue();
    }
}
