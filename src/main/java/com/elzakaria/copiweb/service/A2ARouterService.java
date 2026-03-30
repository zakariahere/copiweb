package com.elzakaria.copiweb.service;

import com.elzakaria.copiweb.agent.CopilotSessionRegistry;
import com.elzakaria.copiweb.agent.SessionEventBridge;
import com.elzakaria.copiweb.dto.A2AActivitySummaryDto;
import com.elzakaria.copiweb.dto.A2AEnvelopeDto;
import com.elzakaria.copiweb.dto.A2ARoutingRequest;
import com.elzakaria.copiweb.dto.A2AThreadDto;
import com.elzakaria.copiweb.dto.AgentCardDto;
import com.elzakaria.copiweb.dto.EventDto;
import com.elzakaria.copiweb.model.*;
import com.github.copilot.sdk.json.MessageOptions;
import com.elzakaria.copiweb.repository.A2AMessageRepository;
import com.elzakaria.copiweb.repository.AgentEventRepository;
import com.elzakaria.copiweb.repository.AgentSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class A2ARouterService {

    private final A2AMessageRepository messageRepo;
    private final AgentSessionRepository sessionRepo;
    private final CopilotSessionRegistry registry;
    private final SseService sseService;
    private final AgentSessionService agentSessionService;
    private final SessionEventBridge eventBridge;
    private final AgentEventRepository eventRepo;

    @Transactional(readOnly = true)
    public List<AgentCardDto> discoverAgents() {
        return sessionRepo.findAllByOrderByLastActiveAtDesc().stream()
                .filter(this::isVisibleInHub)
                .map(this::toAgentCard)
                .toList();
    }

    public AgentCardDto getAgentCard(Long sessionId) {
        var session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));
        return toAgentCard(session);
    }

    @Transactional
    public A2AEnvelopeDto send(A2ARoutingRequest req) {
        var sender = sessionRepo.findById(req.senderSessionId())
                .orElseThrow(() -> new EntityNotFoundException("Sender session not found: " + req.senderSessionId()));

        AgentSession receiver = null;
        if (req.receiverSessionId() != null) {
            receiver = sessionRepo.findById(req.receiverSessionId())
                    .orElseThrow(() -> new EntityNotFoundException("Receiver session not found: " + req.receiverSessionId()));
        }

        var msg = new A2AMessage();
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setCorrelationId(req.correlationId() != null ? req.correlationId() : msg.getMessageId());
        msg.setSenderSession(sender);
        msg.setReceiverSession(receiver);
        msg.setMessageType(resolveMessageType(req.messageType(), receiver));
        msg.setPayload(req.payload());
        msg.setStatus(A2AMessageStatus.PENDING);
        msg = messageRepo.save(msg);

        persistSendReceipt(sender, msg);

        broadcastSend(sender, receiver, msg);

        if (receiver != null) {
            deliver(msg, receiver);
        } else {
            broadcastToAll(sender, msg);
        }

        return toEnvelope(msg);
    }

    @Transactional
    public A2AEnvelopeDto deliver(Long messageId) {
        var msg = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("A2A message not found: " + messageId));

        if (msg.getReceiverSession() == null) {
            throw new IllegalStateException("Cannot deliver a broadcast message by id");
        }

        deliver(msg, msg.getReceiverSession());
        return toEnvelope(msg);
    }

    @Transactional(readOnly = true)
    public List<A2AEnvelopeDto> getPendingMessages(Long sessionId) {
        var session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));
        return messageRepo.findByReceiverSessionAndStatusOrderByCreatedAtAsc(session, A2AMessageStatus.PENDING)
                .stream().map(this::toEnvelope).toList();
    }

    @Transactional(readOnly = true)
    public List<A2AEnvelopeDto> getConversation(String correlationId) {
        return messageRepo.findByCorrelationIdOrderByCreatedAtAsc(correlationId)
                .stream().map(this::toEnvelope).toList();
    }

    @Transactional(readOnly = true)
    public List<A2AEnvelopeDto> getRecentMessages(int limit) {
        return messageRepo.findRecent(limit)
                .stream().map(this::toEnvelope).toList();
    }

    @Transactional(readOnly = true)
    public List<A2AThreadDto> getRecentThreads(int limit) {
        var messages = messageRepo.findRecent(Math.max(limit * 8, 40)).stream()
                .sorted(Comparator.comparing(A2AMessage::getCreatedAt))
                .toList();

        var byCorrelation = new LinkedHashMap<String, List<A2AMessage>>();
        for (var message : messages) {
            byCorrelation.computeIfAbsent(message.getCorrelationId(), ignored -> new ArrayList<>()).add(message);
        }

        return byCorrelation.values().stream()
                .sorted(Comparator.comparing(this::threadUpdatedAt).reversed())
                .limit(limit)
                .map(this::toThread)
                .toList();
    }

    public long countPending() {
        return messageRepo.countByStatus(A2AMessageStatus.PENDING);
    }

    private void deliver(A2AMessage msg, AgentSession receiver) {
        try {
            var handle = agentSessionService.ensureSessionHandle(receiver.getId());

            receiver.setStatus(SessionStatus.ACTIVE);
            sessionRepo.save(receiver);

            if (msg.getReceiverEventSequence() == null) {
                var receiveEvent = eventBridge.persistEvent(
                        receiver.getId(),
                        handle,
                        EventType.A2A_RECEIVE,
                        "system",
                        buildReceiveEventContent(msg),
                        null,
                        null,
                        null
                );
                msg.setReceiverEventSequence(receiveEvent.getSequence());
                messageRepo.save(msg);
            }

            handle.sdkSession().send(new MessageOptions().setPrompt(buildAgentPrompt(msg))).get();

            msg.setStatus(A2AMessageStatus.DELIVERED);
            msg.setDeliveredAt(LocalDateTime.now());
            messageRepo.save(msg);

            sseService.broadcast(handle.sdkSessionId(),
                    EventDto.a2aReceive(String.valueOf(msg.getSenderSession().getId()), msg.getPayload(), handle.sdkSessionId()));
            log.info("A2A message delivered: {} -> {} (correlationId={})",
                    msg.getSenderSession().getName(), receiver.getName(), msg.getCorrelationId());
        } catch (Exception e) {
            msg.setStatus(A2AMessageStatus.FAILED);
            messageRepo.save(msg);
            throw new IllegalStateException(
                    "Failed to deliver A2A message to session " + receiver.getId() + ": " + e.getMessage(), e);
        }
    }

    private void broadcastSend(AgentSession sender, AgentSession receiver, A2AMessage msg) {
        registry.findByDbSessionId(sender.getId()).ifPresent(handle -> {
            String targetId = receiver != null ? String.valueOf(receiver.getId()) : "ALL";
            sseService.broadcast(handle.sdkSessionId(),
                    EventDto.a2aSend(targetId, msg.getPayload(), handle.sdkSessionId()));
        });
    }

    private void broadcastToAll(AgentSession sender, A2AMessage msg) {
        var activeSessions = sessionRepo.findAllByOrderByLastActiveAtDesc().stream()
                .filter(s -> !s.getId().equals(sender.getId()))
                .filter(this::isRoutableSession)
                .toList();

        for (var target : activeSessions) {
            var broadcastCopy = new A2AMessage();
            broadcastCopy.setMessageId(UUID.randomUUID().toString());
            broadcastCopy.setCorrelationId(msg.getCorrelationId());
            broadcastCopy.setSenderSession(sender);
            broadcastCopy.setReceiverSession(target);
            broadcastCopy.setMessageType(A2AMessageType.BROADCAST);
            broadcastCopy.setPayload(msg.getPayload());
            broadcastCopy.setStatus(A2AMessageStatus.PENDING);
            messageRepo.save(broadcastCopy);

            deliver(broadcastCopy, target);
        }

        log.info("A2A broadcast from '{}' delivered to {} sessions", sender.getName(), activeSessions.size());
    }

    private boolean isRoutableSession(AgentSession session) {
        return session.getStatus() == SessionStatus.ACTIVE || session.getStatus() == SessionStatus.IDLE;
    }

    private boolean isVisibleInHub(AgentSession session) {
        return session.getStatus() != SessionStatus.CLOSED;
    }

    private void persistSendReceipt(AgentSession sender, A2AMessage msg) {
        try {
            var handle = agentSessionService.ensureSessionHandle(sender.getId());
            var sendEvent = eventBridge.persistEvent(
                    sender.getId(),
                    handle,
                    EventType.A2A_SEND,
                    "system",
                    buildSendEventContent(msg),
                    null,
                    null,
                    null
            );
            msg.setSenderEventSequence(sendEvent.getSequence());
            messageRepo.save(msg);
        } catch (Exception e) {
            log.debug("Unable to persist A2A send receipt for session {}: {}", sender.getId(), e.getMessage());
        }
    }

    private A2AThreadDto toThread(List<A2AMessage> messages) {
        var first = messages.getFirst();
        var latest = messages.getLast();
        int delivered = (int) messages.stream().filter(msg -> msg.getStatus() == A2AMessageStatus.DELIVERED).count();
        int failed = (int) messages.stream().filter(msg -> msg.getStatus() == A2AMessageStatus.FAILED).count();
        int pending = (int) messages.stream().filter(msg -> msg.getStatus() == A2AMessageStatus.PENDING).count();

        return new A2AThreadDto(
                first.getCorrelationId(),
                first.getSenderSession().getId(),
                first.getSenderSession().getName(),
                latest.getReceiverSession() != null ? latest.getReceiverSession().getId() : null,
                latest.getReceiverSession() != null ? latest.getReceiverSession().getName() : null,
                messages.size(),
                delivered,
                failed,
                pending,
                first.getCreatedAt(),
                threadUpdatedAt(messages),
                messages.stream().map(this::toEnvelope).toList(),
                summarizeReceiverActivity(messages)
        );
    }

    private LocalDateTime threadUpdatedAt(List<A2AMessage> messages) {
        return messages.getLast().getDeliveredAt() != null ? messages.getLast().getDeliveredAt() : messages.getLast().getCreatedAt();
    }

    private A2AActivitySummaryDto summarizeReceiverActivity(List<A2AMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            var msg = messages.get(i);
            if (msg.getReceiverSession() == null || msg.getReceiverEventSequence() == null) {
                continue;
            }

            var events = eventRepo.findTop30BySessionAndSequenceGreaterThanOrderBySequenceAsc(
                    msg.getReceiverSession(), msg.getReceiverEventSequence());
            if (events.isEmpty()) {
                return new A2AActivitySummaryDto(
                        msg.getReceiverSession().getId(),
                        msg.getReceiverSession().getName(),
                        msg.getStatus().name(),
                        null,
                        List.of(),
                        null,
                        msg.getDeliveredAt()
                );
            }

            var tools = new ArrayList<String>();
            String assistantSummary = null;
            String errorSummary = null;
            String processingState = "RUNNING";
            LocalDateTime lastActivityAt = msg.getDeliveredAt();

            for (var event : events) {
                if (event.getEventType() == EventType.USER_MSG || event.getEventType() == EventType.A2A_RECEIVE) {
                    break;
                }

                lastActivityAt = event.getOccurredAt();

                switch (event.getEventType()) {
                    case TOOL_START -> {
                        var label = event.getToolName() != null ? event.getToolName() : "tool";
                        if (!tools.contains(label) && tools.size() < 4) {
                            tools.add(label);
                        }
                    }
                    case TOOL_COMPLETE -> {
                        var label = event.getToolName() != null ? event.getToolName() : "tool";
                        if (!tools.contains(label) && tools.size() < 4) {
                            tools.add(label);
                        }
                    }
                    case ASSISTANT_MSG -> {
                        if (assistantSummary == null && event.getContent() != null && !event.getContent().isBlank()) {
                            assistantSummary = abbreviate(event.getContent(), 200);
                        }
                    }
                    case SESSION_ERROR -> {
                        errorSummary = abbreviate(event.getContent(), 160);
                        processingState = "FAILED";
                    }
                    case IDLE -> {
                        if (!"FAILED".equals(processingState)) {
                            processingState = "COMPLETED";
                        }
                    }
                    default -> {
                    }
                }
            }

            return new A2AActivitySummaryDto(
                    msg.getReceiverSession().getId(),
                    msg.getReceiverSession().getName(),
                    processingState,
                    assistantSummary,
                    tools,
                    errorSummary,
                    lastActivityAt
            );
        }

        return null;
    }

    private String buildSendEventContent(A2AMessage msg) {
        return "A2A send [" + msg.getCorrelationId() + "] to "
                + (msg.getReceiverSession() != null ? msg.getReceiverSession().getName() : "ALL")
                + ": " + abbreviate(msg.getPayload(), 180);
    }

    private String buildReceiveEventContent(A2AMessage msg) {
        return "A2A receive [" + msg.getCorrelationId() + "] from "
                + msg.getSenderSession().getName()
                + ": " + abbreviate(msg.getPayload(), 180);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    String buildAgentPrompt(A2AMessage msg) {
        var sender = msg.getSenderSession();
        var receiver = msg.getReceiverSession();
        return """
                You received an A2A message from another CoPiWeb session.

                Type: %s
                From: %s (dbSessionId=%d)
                To: %s
                Correlation ID: %s

                Payload:
                %s

                If you use tools or produce results, keep your response grounded in this correlation ID so the
                A2A dashboard can summarize the exchange coherently.
                """.formatted(
                msg.getMessageType().name(),
                sender.getName(),
                sender.getId(),
                receiver != null ? receiver.getName() + " (dbSessionId=" + receiver.getId() + ")" : "ALL",
                msg.getCorrelationId(),
                msg.getPayload()
        );
    }

    private A2AMessageType resolveMessageType(String requested, AgentSession receiver) {
        if (requested != null) {
            try {
                return A2AMessageType.valueOf(requested);
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return receiver != null ? A2AMessageType.TASK_REQUEST : A2AMessageType.BROADCAST;
    }

    AgentCardDto toAgentCard(AgentSession session) {
        boolean routable = isRoutableSession(session);
        List<String> capabilities = routable ? List.of("chat", "streaming", "tools") : List.of("chat");

        return new AgentCardDto(
                session.getId(),
                session.getSessionId(),
                session.getName(),
                session.getModel(),
                session.getSelectedAgentName(),
                session.getSelectedAgentDisplayName(),
                session.getStatus(),
                capabilities,
                routable
        );
    }

    A2AEnvelopeDto toEnvelope(A2AMessage msg) {
        return new A2AEnvelopeDto(
                msg.getMessageId(),
                msg.getCorrelationId(),
                msg.getSenderSession().getId(),
                msg.getSenderSession().getName(),
                msg.getReceiverSession() != null ? msg.getReceiverSession().getId() : null,
                msg.getReceiverSession() != null ? msg.getReceiverSession().getName() : null,
                msg.getMessageType().name(),
                msg.getPayload(),
                msg.getStatus().name(),
                msg.getCreatedAt(),
                msg.getDeliveredAt()
        );
    }
}
