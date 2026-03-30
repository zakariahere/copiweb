package com.elzakaria.copiweb.service;

import com.elzakaria.copiweb.dto.AgentCardDto;
import com.elzakaria.copiweb.model.AgentSession;
import com.elzakaria.copiweb.model.SessionStatus;
import com.elzakaria.copiweb.agent.CopilotSessionRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    }
}
