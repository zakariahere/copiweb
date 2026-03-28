package com.elzakaria.copiweb.agent;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CopilotSessionRegistry {

    private final ConcurrentHashMap<String, CopilotSessionHandle> handles = new ConcurrentHashMap<>();

    public void register(CopilotSessionHandle handle) {
        handles.put(handle.sdkSessionId(), handle);
    }

    public Optional<CopilotSessionHandle> findBySdkSessionId(String sdkSessionId) {
        return Optional.ofNullable(handles.get(sdkSessionId));
    }

    public Optional<CopilotSessionHandle> findByDbSessionId(Long dbSessionId) {
        return handles.values().stream()
            .filter(h -> h.dbSessionId().equals(dbSessionId))
            .findFirst();
    }

    public void remove(String sdkSessionId) {
        handles.remove(sdkSessionId);
    }

    public Collection<CopilotSessionHandle> all() {
        return handles.values();
    }

    public boolean isActive(String sdkSessionId) {
        return handles.containsKey(sdkSessionId);
    }
}
