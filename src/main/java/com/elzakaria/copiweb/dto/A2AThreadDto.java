package com.elzakaria.copiweb.dto;

import java.time.LocalDateTime;
import java.util.List;

public record A2AThreadDto(
    String correlationId,
    Long senderSessionId,
    String senderName,
    Long receiverSessionId,
    String receiverName,
    int messageCount,
    int deliveredCount,
    int failedCount,
    int pendingCount,
    LocalDateTime startedAt,
    LocalDateTime updatedAt,
    List<A2AEnvelopeDto> messages,
    A2AActivitySummaryDto activitySummary
) {}
