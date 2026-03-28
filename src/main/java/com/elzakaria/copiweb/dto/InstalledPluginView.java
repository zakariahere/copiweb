package com.elzakaria.copiweb.dto;

public record InstalledPluginView(
    String name,
    String description,
    String version,
    String source,
    boolean declaresAgents,
    boolean hasAgentFiles,
    boolean hasSkills,
    boolean hasHooks,
    boolean hasMcpServers
) {
}
