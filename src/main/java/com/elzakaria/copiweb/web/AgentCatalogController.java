package com.elzakaria.copiweb.web;

import com.elzakaria.copiweb.service.AgentDiscoveryService;
import com.elzakaria.copiweb.service.WorkflowCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AgentCatalogController {

    private final AgentDiscoveryService agentDiscoveryService;
    private final WorkflowCommandService commandService;

    @GetMapping("/agents")
    public String listAgents(Model model) {
        var catalog = agentDiscoveryService.getCatalog();
        model.addAttribute("catalog", catalog);
        model.addAttribute("agents", catalog.agents());
        model.addAttribute("installedPlugins", catalog.installedPlugins());
        model.addAttribute("marketplaces", catalog.marketplaces());
        return "agents/list";
    }

    @GetMapping("/commands")
    public String listCommands(Model model) {
        model.addAttribute("commands", commandService.listCommands());
        return "commands/list";
    }
}
