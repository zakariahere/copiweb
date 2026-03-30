package com.elzakaria.copiweb.repository;

import com.elzakaria.copiweb.model.AgentEvent;
import com.elzakaria.copiweb.model.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AgentEventRepository extends JpaRepository<AgentEvent, Long> {

    List<AgentEvent> findBySessionOrderBySequenceAsc(AgentSession session);

    List<AgentEvent> findTop50BySessionOrderBySequenceDesc(AgentSession session);

    long countByOccurredAtAfter(LocalDateTime since);

    @Query("SELECT e FROM AgentEvent e WHERE e.session = :session ORDER BY e.sequence DESC LIMIT :limit")
    List<AgentEvent> findRecentBySession(AgentSession session, int limit);

    @Query("""
        SELECT e FROM AgentEvent e
        WHERE e.occurredAt >= :since
        ORDER BY e.occurredAt DESC
        LIMIT 10
        """)
    List<AgentEvent> findRecentEventsAcrossAllSessions(LocalDateTime since);

    List<AgentEvent> findTop30BySessionAndSequenceGreaterThanOrderBySequenceAsc(AgentSession session, int sequence);
}
