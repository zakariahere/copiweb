package com.elzakaria.copiweb.repository;

import com.elzakaria.copiweb.model.A2AMessage;
import com.elzakaria.copiweb.model.A2AMessageStatus;
import com.elzakaria.copiweb.model.AgentSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface A2AMessageRepository extends JpaRepository<A2AMessage, Long> {

    Optional<A2AMessage> findByMessageId(String messageId);

    @EntityGraph(attributePaths = {"senderSession", "receiverSession"})
    List<A2AMessage> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);

    @EntityGraph(attributePaths = {"senderSession", "receiverSession"})
    List<A2AMessage> findByReceiverSessionAndStatusOrderByCreatedAtAsc(AgentSession receiver, A2AMessageStatus status);

    @EntityGraph(attributePaths = {"senderSession", "receiverSession"})
    @Query("""
        SELECT m FROM A2AMessage m
        WHERE m.senderSession = :session OR m.receiverSession = :session
        ORDER BY m.createdAt DESC
        LIMIT :limit
        """)
    List<A2AMessage> findRecentBySession(AgentSession session, int limit);

    @EntityGraph(attributePaths = {"senderSession", "receiverSession"})
    @Query("SELECT m FROM A2AMessage m ORDER BY m.createdAt DESC LIMIT :limit")
    List<A2AMessage> findRecent(int limit);

    long countByStatus(A2AMessageStatus status);
}
