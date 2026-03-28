package com.elzakaria.copiweb.web;

import com.elzakaria.copiweb.dto.CreateSessionRequest;
import com.elzakaria.copiweb.dto.SendMessageRequest;
import com.elzakaria.copiweb.dto.SessionSummaryDto;
import com.elzakaria.copiweb.model.AgentEvent;
import com.elzakaria.copiweb.model.AgentSession;
import com.elzakaria.copiweb.service.AgentSessionService;
import com.elzakaria.copiweb.service.ModelService;
import com.github.copilot.sdk.json.ModelInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SessionApiController {

    private final AgentSessionService sessionService;
    private final ModelService modelService;

    @GetMapping("/sessions")
    public List<SessionSummaryDto> listSessions() {
        return sessionService.listSessions().stream()
            .map(s -> new SessionSummaryDto(
                s.getId(), s.getSessionId(), s.getName(), s.getModel(),
                s.getSelectedAgentName(), s.getSelectedAgentDisplayName(),
                s.getStatus(), s.getTurnCount(), s.getCreatedAt(), s.getLastActiveAt()))
            .toList();
    }

    @PostMapping("/sessions")
    public ResponseEntity<AgentSession> createSession(@Valid @RequestBody CreateSessionRequest req) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(req));
    }

    @GetMapping("/sessions/{id}")
    public AgentSession getSession(@PathVariable Long id) {
        return sessionService.getSession(id);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) throws Exception {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{id}/message")
    public ResponseEntity<Void> sendMessage(@PathVariable Long id,
                                             @Valid @RequestBody SendMessageRequest req) throws Exception {
        sessionService.sendMessage(id, req);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/sessions/{id}/abort")
    public ResponseEntity<Void> abort(@PathVariable Long id) throws Exception {
        sessionService.abortSession(id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/sessions/{id}/history")
    public List<AgentEvent> getHistory(@PathVariable Long id) {
        return sessionService.getHistory(id);
    }

    @GetMapping("/models")
    public List<ModelInfo> getModels() {
        return modelService.getModels();
    }

    @PostMapping("/models/refresh")
    public ResponseEntity<Void> refreshModels() {
        modelService.refreshModels();
        return ResponseEntity.ok().build();
    }
}
