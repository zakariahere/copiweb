package com.elzakaria.copiweb.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "agent_event",
    indexes = @Index(name = "idx_event_session_sequence", columnList = "session_db_id, sequence")
)
@Getter
@Setter
public class AgentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_db_id", nullable = false)
    private AgentSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private EventType eventType;

    @Column(length = 32)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "tool_name", length = 255)
    private String toolName;

    @Column(name = "tool_args", columnDefinition = "TEXT")
    private String toolArgs;

    @Column(name = "tool_result", columnDefinition = "TEXT")
    private String toolResult;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Column(nullable = false)
    private int sequence;
}
