package com.elzakaria.copiweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSessionRequest(
    @NotBlank String name,
    @NotBlank String model,
    String systemPrompt,
    String workingDirectory,
    @NotNull Boolean streaming,
    String agentName
) {
    public @NotNull Boolean streaming() {
        return streaming != null && streaming;
    }
}
