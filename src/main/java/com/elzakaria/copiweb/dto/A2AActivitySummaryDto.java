package com.elzakaria.copiweb.dto;

import java.time.LocalDateTime;
import java.util.List;

public record A2AActivitySummaryDto(
    Long receiverSessionId,
    String receiverName,
    String processingState,
    String assistantSummary,
    List<String> toolSummaries,
    String errorSummary,
    LocalDateTime lastActivityAt
) {}
