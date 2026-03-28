package com.elzakaria.copiweb.service;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.ModelInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelService {

    private final CopilotClient copilotClient;

    private volatile List<ModelInfo> cachedModels = List.of();

    public void refreshModels() {
        try {
            cachedModels = copilotClient.listModels().get();
            log.info("Loaded {} models from Copilot SDK", cachedModels.size());
        } catch (Exception e) {
            log.warn("Failed to refresh model list: {}", e.getMessage());
        }
    }

    public List<ModelInfo> getModels() {
        return cachedModels;
    }
}
