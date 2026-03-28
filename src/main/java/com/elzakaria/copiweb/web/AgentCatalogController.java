package com.elzakaria.copiweb.web;

import com.elzakaria.copiweb.service.AgentProfileService;
import com.elzakaria.copiweb.service.WorkflowCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AgentCatalogController {

    private final AgentProfileService profileService;
    private final WorkflowCommandService commandService;

    @GetMapping("/agents")
    public String listAgents(Model model) {
        model.addAttribute("agents", profileService.listProfiles());
        return "agents/list";
    }

    @GetMapping("/commands")
    public String listCommands(Model model) {
        model.addAttribute("commands", commandService.listCommands());
        return "commands/list";
    }

    @PostMapping("/agents/{id}/delete")
    public String deleteAgent(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            profileService.deleteProfile(id);
            redirectAttrs.addFlashAttribute("success", "Agent profile deleted.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agents";
    }
}
