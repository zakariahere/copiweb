package com.elzakaria.copiweb.service;

import com.elzakaria.copiweb.agent.CopilotSessionHandle;
import com.elzakaria.copiweb.agent.CopilotSessionRegistry;
import com.elzakaria.copiweb.agent.SessionEventBridge;
import com.elzakaria.copiweb.dto.CreateSessionRequest;
import com.elzakaria.copiweb.dto.SendMessageRequest;
import com.elzakaria.copiweb.model.AgentEvent;
import com.elzakaria.copiweb.model.AgentSession;
import com.elzakaria.copiweb.model.EventType;
import com.elzakaria.copiweb.model.SessionStatus;
import com.elzakaria.copiweb.repository.AgentEventRepository;
import com.elzakaria.copiweb.repository.AgentSessionRepository;
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.SystemMessageMode;
import com.github.copilot.sdk.json.*;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentSessionService {

    private final CopilotClient copilotClient;
    private final ModelService modelService;
    private final AgentSessionRepository sessionRepo;
    private final AgentEventRepository eventRepo;
    private final CopilotSessionRegistry registry;
    private final SessionEventBridge eventBridge;
    private final SseService sseService;

    @PostConstruct
    public void init() throws Exception {
        log.info("Starting CopilotClient...");
        copilotClient.start().get(30, TimeUnit.SECONDS);
        var status = copilotClient.getStatus().get();
        log.info("CopilotClient started. CLI version: {}", status.getVersion());
        modelService.refreshModels();
    }

    public AgentSession createSession(CreateSessionRequest req) throws Exception {
        var config = new SessionConfig()
            .setModel(req.model())
            .setStreaming(req.streaming())
            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

        if (req.systemPrompt() != null && !req.systemPrompt().isBlank()) {
            config.setSystemMessage(new SystemMessageConfig()
                .setMode(SystemMessageMode.APPEND)
                .setContent(req.systemPrompt()));
        }

        if (req.workingDirectory() != null && !req.workingDirectory().isBlank()) {
            config.setWorkingDirectory(req.workingDirectory());
        }

        // Call SDK first so we have the session_id before any DB write
        var sdkSession = copilotClient.createSession(config).get();
        String sdkId = sdkSession.getSessionId();

        var dbSession = new AgentSession();
        dbSession.setSessionId(sdkId);
        dbSession.setName(req.name());
        dbSession.setModel(req.model());
        dbSession.setSystemPrompt(req.systemPrompt());
        dbSession.setWorkingDirectory(req.workingDirectory());
        dbSession.setStreaming(req.streaming());
        dbSession.setStatus(SessionStatus.ACTIVE);
        dbSession = sessionRepo.save(dbSession);

        var handle = new CopilotSessionHandle(sdkSession, sdkId, dbSession.getId(), new AtomicInteger(0));
        registry.register(handle);
        eventBridge.wireEvents(sdkSession, handle);

        log.info("Created session: dbId={} sdkId={} model={}", dbSession.getId(), sdkId, req.model());
        return dbSession;
    }

    @Transactional
    public AgentSession resumeSession(Long dbId) throws Exception {
        var dbSession = sessionRepo.findById(dbId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found: " + dbId));

        var sdkSession = copilotClient.resumeSession(
            dbSession.getSessionId(),
            new ResumeSessionConfig().setModel(dbSession.getModel())
        ).get();

        dbSession.setStatus(SessionStatus.ACTIVE);
        dbSession = sessionRepo.save(dbSession);

        int nextSeq = eventRepo.findBySessionOrderBySequenceAsc(dbSession)
            .stream().mapToInt(AgentEvent::getSequence).max().orElse(-1) + 1;

        var handle = new CopilotSessionHandle(sdkSession, dbSession.getSessionId(), dbId, new AtomicInteger(nextSeq));
        registry.register(handle);
        eventBridge.wireEvents(sdkSession, handle);

        log.info("Resumed session: dbId={} sdkId={}", dbId, dbSession.getSessionId());
        return dbSession;
    }

    public void sendMessage(Long dbId, SendMessageRequest req) throws Exception {
        var dbSession = sessionRepo.findById(dbId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found: " + dbId));

        var handle = registry.findByDbSessionId(dbId)
            .orElseThrow(() -> new IllegalStateException("Session not active in registry: " + dbId));

        // Persist user message
        eventBridge.persistEventAsync(dbId, handle, EventType.USER_MSG, "user", req.message(), null, null, null);

        // Update status to ACTIVE before sending
        dbSession.setStatus(SessionStatus.ACTIVE);
        sessionRepo.save(dbSession);

        handle.sdkSession().send(new MessageOptions().setPrompt(req.message())).get();
    }

    public void abortSession(Long dbId) throws Exception {
        registry.findByDbSessionId(dbId).ifPresent(handle -> {
            try {
                handle.sdkSession().abort().get();
            } catch (Exception e) {
                log.warn("Abort failed for session {}: {}", dbId, e.getMessage());
            }
        });
    }

    @Transactional
    public void deleteSession(Long dbId) throws Exception {
        var dbSession = sessionRepo.findById(dbId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found: " + dbId));

        String sdkSessionId = dbSession.getSessionId();

        try {
            copilotClient.deleteSession(sdkSessionId).get();
        } catch (Exception e) {
            log.warn("SDK delete failed for session {} (sdkId={}): {}", dbId, sdkSessionId, e.getMessage());
        }

        registry.remove(sdkSessionId);
        sseService.removeAll(sdkSessionId);

        sessionRepo.delete(dbSession);
        sessionRepo.flush();
        log.info("Deleted session dbId={} sdkId={}", dbId, sdkSessionId);
    }

    public List<AgentSession> listSessions() {
        return sessionRepo.findAllByOrderByLastActiveAtDesc();
    }

    public AgentSession getSession(Long dbId) {
        return sessionRepo.findById(dbId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found: " + dbId));
    }

    public List<AgentEvent> getHistory(Long dbId) {
        var session = getSession(dbId);
        return eventRepo.findBySessionOrderBySequenceAsc(session);
    }

    public List<AgentEvent> getRecentEvents(Long dbId, int limit) {
        var session = getSession(dbId);
        var events = eventRepo.findRecentBySession(session, limit);
        // Return in ascending order for display
        return events.reversed();
    }
}
