package com.elzakaria.copiweb.repository;

import com.elzakaria.copiweb.model.AgentSession;
import com.elzakaria.copiweb.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgentSessionRepository extends JpaRepository<AgentSession, Long> {

    Optional<AgentSession> findBySessionId(String sessionId);

    List<AgentSession> findAllByOrderByLastActiveAtDesc();

    long countByStatus(SessionStatus status);

    long countByStatusIn(List<SessionStatus> statuses);

    @Query("SELECT s FROM AgentSession s ORDER BY s.lastActiveAt DESC LIMIT 10")
    List<AgentSession> findTop10ByOrderByLastActiveAtDesc();
}
