package com.elzakaria.copiweb.dto;

import com.elzakaria.copiweb.model.SessionStatus;

import java.time.LocalDateTime;

public record SessionSummaryDto(
    Long id,
    String sessionId,
    String name,
    String model,
    String selectedAgentName,
    String selectedAgentDisplayName,
    SessionStatus status,
    int turnCount,
    LocalDateTime createdAt,
    LocalDateTime lastActiveAt
) {}
