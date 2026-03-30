package com.elzakaria.copiweb.web;

import com.elzakaria.copiweb.dto.A2AEnvelopeDto;
import com.elzakaria.copiweb.dto.A2ARoutingRequest;
import com.elzakaria.copiweb.dto.AgentCardDto;
import com.elzakaria.copiweb.service.A2ARouterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/a2a")
@RequiredArgsConstructor
@Slf4j
public class A2AApiController {

    private final A2ARouterService routerService;

    @GetMapping("/agents")
    public List<AgentCardDto> discoverAgents() {
        return routerService.discoverAgents();
    }

    @GetMapping("/agents/{sessionId}/card")
    public AgentCardDto getAgentCard(@PathVariable Long sessionId) {
        return routerService.getAgentCard(sessionId);
    }

    @PostMapping("/send")
    public ResponseEntity<A2AEnvelopeDto> send(@Valid @RequestBody A2ARoutingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routerService.send(req));
    }

    @PostMapping("/messages/{id}/deliver")
    public A2AEnvelopeDto deliver(@PathVariable Long id) {
        return routerService.deliver(id);
    }

    @GetMapping("/sessions/{sessionId}/pending")
    public List<A2AEnvelopeDto> getPending(@PathVariable Long sessionId) {
        return routerService.getPendingMessages(sessionId);
    }

    @GetMapping("/conversations/{correlationId}")
    public List<A2AEnvelopeDto> getConversation(@PathVariable String correlationId) {
        return routerService.getConversation(correlationId);
    }

    @GetMapping("/messages/recent")
    public List<A2AEnvelopeDto> getRecentMessages(@RequestParam(defaultValue = "50") int limit) {
        return routerService.getRecentMessages(limit);
    }
}
