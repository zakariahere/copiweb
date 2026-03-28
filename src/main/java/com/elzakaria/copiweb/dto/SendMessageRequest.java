package com.elzakaria.copiweb.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
    @NotBlank String message
) {}
