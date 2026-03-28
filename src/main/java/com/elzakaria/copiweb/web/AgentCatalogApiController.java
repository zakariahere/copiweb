package com.elzakaria.copiweb.web;

import com.elzakaria.copiweb.model.AgentProfile;
import com.elzakaria.copiweb.model.WorkflowCommand;
import com.elzakaria.copiweb.service.AgentProfileService;
import com.elzakaria.copiweb.service.WorkflowCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AgentCatalogApiController {

    private final AgentProfileService profileService;
    private final WorkflowCommandService commandService;

    @GetMapping("/agents")
    public List<AgentProfile> listAgents() {
        return profileService.listProfiles();
    }

    @PostMapping("/agents")
    public AgentProfile createAgent(@RequestBody AgentProfile profile) {
        return profileService.createProfile(profile);
    }

    @DeleteMapping("/agents/{id}")
    public ResponseEntity<Void> deleteAgent(@PathVariable Long id) {
        profileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
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
