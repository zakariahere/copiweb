package com.elzakaria.copiweb.web;

import com.elzakaria.copiweb.dto.AgentCatalogView;
import com.elzakaria.copiweb.dto.DiscoveredAgentView;
import com.elzakaria.copiweb.model.WorkflowCommand;
import com.elzakaria.copiweb.service.AgentDiscoveryService;
import com.elzakaria.copiweb.service.WorkflowCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AgentCatalogApiController {

    private final AgentDiscoveryService agentDiscoveryService;
    private final WorkflowCommandService commandService;

    @GetMapping("/agents")
    public List<DiscoveredAgentView> listAgents() {
        return agentDiscoveryService.listAgents();
    }

    @GetMapping("/agent-catalog")
    public AgentCatalogView getAgentCatalog() {
        return agentDiscoveryService.getCatalog();
    }

    @GetMapping("/commands")
    public List<WorkflowCommand> listCommands() {
        return commandService.listCommands();
    }

    @PostMapping("/commands/{id}/assemble")
    public Map<String, String> assembleCommand(@PathVariable Long id,
                                               @RequestBody Map<String, String> params) {
        return Map.of("prompt", commandService.assemblePrompt(id, params));
    }
}
