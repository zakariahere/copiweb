package com.elzakaria.copiweb.dto;

public record ToolView(
        String name,
        String description,
        int parameterCount,
        boolean overridesBuiltin,
        boolean skipPermission
) {}
