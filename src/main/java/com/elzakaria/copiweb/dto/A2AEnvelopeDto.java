package com.elzakaria.copiweb.dto;

import java.time.LocalDateTime;

public record A2AEnvelopeDto(
    String messageId,
    String correlationId,
    Long senderSessionId,
    String senderName,
    Long receiverSessionId,
    String receiverName,
    String messageType,
    String payload,
    String status,
    LocalDateTime createdAt,
    LocalDateTime deliveredAt
) {}
