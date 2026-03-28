package com.elzakaria.copiweb.dto;

public record DiscoveredAgentView(
    String name,
    String displayName,
    String description,
    boolean selected
) {
}
