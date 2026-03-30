package com.elzakaria.copiweb.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "a2a_message",
    indexes = {
        @Index(name = "idx_a2a_correlation", columnList = "correlation_id"),
        @Index(name = "idx_a2a_receiver_status", columnList = "receiver_session_id, status")
    }
)
@Getter
@Setter
public class A2AMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 64)
    private String messageId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_session_id", nullable = false)
    private AgentSession senderSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_session_id")
    private AgentSession receiverSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 32)
    private A2AMessageType messageType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private A2AMessageStatus status = A2AMessageStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "sender_event_sequence")
    private Integer senderEventSequence;

    @Column(name = "receiver_event_sequence")
    private Integer receiverEventSequence;
}
