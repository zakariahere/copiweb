package com.elzakaria.copiweb.service;

import com.elzakaria.copiweb.dto.AgentCardDto;
import com.elzakaria.copiweb.model.A2AMessage;
import com.elzakaria.copiweb.model.A2AMessageType;
import com.elzakaria.copiweb.model.AgentSession;
import com.elzakaria.copiweb.model.SessionStatus;
import com.elzakaria.copiweb.agent.CopilotSessionRegistry;
import com.elzakaria.copiweb.agent.CopilotSessionHandle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class A2ARouterServiceTest {

    private final CopilotSessionRegistry registry = new CopilotSessionRegistry();
    private final A2ARouterService service = new A2ARouterService(null, null, registry, null);

    @Test
    void toAgentCardMapsSessionFieldsCorrectly() {
        var session = new AgentSession();
        session.setId(1L);
        session.setSessionId("sdk-123");
        session.setName("test-agent");
        session.setModel("gpt-4.1");
        session.setSelectedAgentName("custom-agent");
        session.setSelectedAgentDisplayName("Custom Agent");
        session.setStatus(SessionStatus.ACTIVE);

        AgentCardDto card = service.toAgentCard(session);

        assertThat(card.sessionId()).isEqualTo(1L);
        assertThat(card.sdkSessionId()).isEqualTo("sdk-123");
        assertThat(card.name()).isEqualTo("test-agent");
        assertThat(card.model()).isEqualTo("gpt-4.1");
        assertThat(card.agentName()).isEqualTo("custom-agent");
        assertThat(card.agentDisplayName()).isEqualTo("Custom Agent");
        assertThat(card.status()).isEqualTo(SessionStatus.ACTIVE);
        // No active handle in registry, so only "chat" capability
        assertThat(card.capabilities()).containsExactly("chat");
        assertThat(card.routable()).isFalse();
    }

    @Test
    void toAgentCardReturnsMinimalCapabilitiesWhenNotInRegistry() {
        var session = new AgentSession();
        session.setId(99L);
        session.setSessionId("sdk-absent");
        session.setName("offline-agent");
        session.setModel("claude-sonnet-4");
        session.setStatus(SessionStatus.IDLE);

        AgentCardDto card = service.toAgentCard(session);

        assertThat(card.capabilities()).containsExactly("chat");
        assertThat(card.status()).isEqualTo(SessionStatus.IDLE);
        assertThat(card.routable()).isFalse();
    }

    @Test
    void toAgentCardReturnsFullCapabilitiesWhenSessionIsRegistered() {
        var session = new AgentSession();
        session.setId(7L);
        session.setSessionId("sdk-live");
        session.setName("live-agent");
        session.setModel("gpt-4.1");
        session.setStatus(SessionStatus.ACTIVE);

        registry.register(new CopilotSessionHandle(null, "sdk-live", 7L, new AtomicInteger(1)));

        AgentCardDto card = service.toAgentCard(session);

        assertThat(card.capabilities()).containsExactly("chat", "streaming", "tools");
        assertThat(card.routable()).isTrue();
    }

    @Test
    void buildAgentPromptIncludesRoutingContext() {
        var sender = new AgentSession();
        sender.setId(1L);
        sender.setName("Architect");

        var receiver = new AgentSession();
        receiver.setId(2L);
        receiver.setName("Implementer");

        var message = new A2AMessage();
        message.setMessageType(A2AMessageType.TASK_REQUEST);
        message.setCorrelationId("corr-123");
        message.setSenderSession(sender);
        message.setReceiverSession(receiver);
        message.setPayload("Investigate the API contract.");

        String prompt = service.buildAgentPrompt(message);

        assertThat(prompt).contains("Type: TASK_REQUEST");
        assertThat(prompt).contains("From: Architect (dbSessionId=1)");
        assertThat(prompt).contains("To: Implementer (dbSessionId=2)");
        assertThat(prompt).contains("Correlation ID: corr-123");
        assertThat(prompt).contains("Investigate the API contract.");
    }
}
