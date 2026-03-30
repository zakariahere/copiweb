package com.elzakaria.copiweb.agent;

import com.elzakaria.copiweb.dto.EventDto;
import com.elzakaria.copiweb.model.AgentEvent;
import com.elzakaria.copiweb.model.AgentSession;
import com.elzakaria.copiweb.model.EventType;
import com.elzakaria.copiweb.model.SessionStatus;
import com.elzakaria.copiweb.repository.AgentEventRepository;
import com.elzakaria.copiweb.repository.AgentSessionRepository;
import com.elzakaria.copiweb.service.SseService;
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventBridge {

    private final AgentSessionRepository sessionRepo;
    private final AgentEventRepository eventRepo;
    private final SseService sseService;

    public void wireEvents(CopilotSession sdkSession, CopilotSessionHandle handle) {
        String sdkId = handle.sdkSessionId();
        Long dbId = handle.dbSessionId();

        sdkSession.on(AssistantMessageDeltaEvent.class, delta -> {
            String chunk = delta.getData().deltaContent();
            sseService.broadcast(sdkId, EventDto.delta(chunk, sdkId));
        });

        sdkSession.on(AssistantMessageEvent.class, msg -> {
            String content = msg.getData().content();
            sseService.broadcast(sdkId, EventDto.assistantMessage(content, sdkId));
            persistEventAsync(dbId, handle, EventType.ASSISTANT_MSG, "assistant", content, null, null, null);
        });

        sdkSession.on(SessionIdleEvent.class, idle -> {
            sseService.broadcast(sdkId, EventDto.idle(sdkId));
            persistEventAsync(dbId, handle, EventType.IDLE, "system", "Session became idle", null, null, null);
            updateSessionOnIdle(dbId);
        });

        sdkSession.on(ToolExecutionStartEvent.class, evt -> {
            String toolName = evt.getData().toolName();
            String toolCallId = evt.getData().toolCallId();
            String args = evt.getData().arguments() != null ? evt.getData().arguments().toString() : null;
            sseService.broadcast(sdkId, EventDto.toolStart(toolName, toolCallId, args, sdkId));
            persistEventAsync(dbId, handle, EventType.TOOL_START, "tool", null, toolName, args, null);
        });

        sdkSession.on(ToolExecutionCompleteEvent.class, evt -> {
            var resultObj = evt.getData().result();
            String result = resultObj != null ? resultObj.content() : null;
            // No toolName on complete event — use toolCallId as identifier
            String toolCallId = evt.getData().toolCallId();
            sseService.broadcast(sdkId, EventDto.toolComplete(toolCallId, result, sdkId));
            persistEventAsync(dbId, handle, EventType.TOOL_COMPLETE, "tool", null, toolCallId, null, result);
        });

        sdkSession.on(SubagentStartedEvent.class, evt -> {
            String agentName = evt.getData().agentName();
            sseService.broadcast(sdkId, EventDto.subagentStart(sdkId));
            persistEventAsync(dbId, handle, EventType.SUBAGENT_START, "system",
                "Subagent started: " + agentName, null, null, null);
        });

        sdkSession.on(SubagentCompletedEvent.class, evt -> {
            String agentName = evt.getData().agentName();
            sseService.broadcast(sdkId, EventDto.subagentComplete(sdkId));
            persistEventAsync(dbId, handle, EventType.SUBAGENT_COMPLETE, "system",
                "Subagent completed: " + agentName, null, null, null);
        });

        sdkSession.on(SessionErrorEvent.class, err -> {
            String message = err.getData().message();
            sseService.broadcast(sdkId, EventDto.error(message, sdkId));
            persistEventAsync(dbId, handle, EventType.SESSION_ERROR, "system", message, null, null, null);
            updateSessionOnError(dbId, message);
        });

        sdkSession.on(AbortEvent.class, evt -> {
            sseService.broadcast(sdkId, EventDto.abort(sdkId));
        });
    }

    @Async
    public void persistEventAsync(Long dbId, CopilotSessionHandle handle,
                                   EventType type, String role, String content,
                                   String toolName, String toolArgs, String toolResult) {
        try {
            persistEvent(dbId, handle, type, role, content, toolName, toolArgs, toolResult);
        } catch (Exception e) {
            log.error("Failed to persist event {} for session {}: {}", type, dbId, e.getMessage(), e);
        }
    }

    @Transactional
    public AgentEvent persistEvent(Long dbId, CopilotSessionHandle handle,
                                   EventType type, String role, String content,
                                   String toolName, String toolArgs, String toolResult) {
        var session = sessionRepo.findById(dbId)
                .orElseThrow(() -> new IllegalStateException("Session not found for event persistence: " + dbId));

        var event = new AgentEvent();
        event.setSession(session);
        event.setEventType(type);
        event.setRole(role);
        event.setContent(content);
        event.setToolName(toolName);
        event.setToolArgs(toolArgs);
        event.setToolResult(toolResult);
        event.setOccurredAt(LocalDateTime.now());
        event.setSequence(handle.nextSequence());
        return eventRepo.save(event);
    }

    @Async
    @Transactional
    public void updateSessionOnIdle(Long dbId) {
        sessionRepo.findById(dbId).ifPresent(session -> {
            session.setStatus(SessionStatus.IDLE);
            session.setTurnCount(session.getTurnCount() + 1);
            sessionRepo.save(session);
        });
    }

    @Async
    @Transactional
    public void updateSessionOnError(Long dbId, String message) {
        sessionRepo.findById(dbId).ifPresent(session -> {
            session.setStatus(SessionStatus.ERROR);
            session.setErrorMessage(message);
            sessionRepo.save(session);
        });
    }
}
