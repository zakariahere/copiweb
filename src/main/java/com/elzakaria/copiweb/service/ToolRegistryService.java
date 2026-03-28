package com.elzakaria.copiweb.service;

import com.elzakaria.copiweb.dto.ToolView;
import com.github.copilot.sdk.json.ToolDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ToolRegistryService {

    private final List<ToolDefinition> tools;

    public List<ToolView> getToolViews() {
        return tools.stream()
                .map(t -> new ToolView(
                        t.name(),
                        t.description(),
                        resolveParameterCount(t),
                        Boolean.TRUE.equals(t.overridesBuiltInTool()),
                        Boolean.TRUE.equals(t.skipPermission())
                ))
                .toList();
    }

    public int count() {
        return tools.size();
    }

    private int resolveParameterCount(ToolDefinition t) {
        if (t.parameters() instanceof Map<?, ?> params) {
            Object props = params.get("properties");
            if (props instanceof Map<?, ?> map) return map.size();
        }
        return 0;
    }
}
