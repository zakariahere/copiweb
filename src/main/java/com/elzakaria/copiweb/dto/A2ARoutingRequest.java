package com.elzakaria.copiweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record A2ARoutingRequest(
    @NotNull Long senderSessionId,
    Long receiverSessionId,
    @NotBlank String payload,
    String messageType,
    String correlationId
) {}
