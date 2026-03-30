package com.elzakaria.copiweb.dto;

import com.elzakaria.copiweb.model.SessionStatus;

import java.util.List;

public record AgentCardDto(
    Long sessionId,
    String sdkSessionId,
    String name,
    String model,
    String agentName,
    String agentDisplayName,
    SessionStatus status,
    List<String> capabilities,
    boolean routable
) {}
