package com.elzakaria.copiweb.service;

import com.elzakaria.copiweb.dto.EventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
@Slf4j
public class SseService {

    private static final long SSE_TIMEOUT_MILLIS = 86_400_000L;

    private final ConcurrentHashMap<String, CopyOnWriteArraySet<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String sdkSessionId) {
        var emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        var sessionEmitters = emitters.computeIfAbsent(sdkSessionId, k -> new CopyOnWriteArraySet<>());
        sessionEmitters.add(emitter);

        Runnable cleanup = () -> {
            var set = emitters.get(sdkSessionId);
            if (set != null) {
                set.remove(emitter);
            }
        };
        emitter.onTimeout(() -> {
            log.debug("SSE emitter timed out for session {}", sdkSessionId);
            cleanup.run();
            emitter.complete();
        });
        emitter.onError(e -> cleanup.run());
        emitter.onCompletion(cleanup);

        log.debug("SSE subscriber added for session {}, total={}, timeoutMs={}",
            sdkSessionId, sessionEmitters.size(), SSE_TIMEOUT_MILLIS);
        return emitter;
    }

    public void broadcast(String sdkSessionId, EventDto payload) {
        var sessionEmitters = emitters.get(sdkSessionId);
        if (sessionEmitters == null || sessionEmitters.isEmpty()) return;

        for (SseEmitter emitter : sessionEmitters) {
            try {
                emitter.send(SseEmitter.event()
                    .data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                log.debug("Removing dead SSE emitter for session {}: {}", sdkSessionId, e.getMessage());
                sessionEmitters.remove(emitter);
            }
        }
    }

    public void removeAll(String sdkSessionId) {
        var sessionEmitters = emitters.remove(sdkSessionId);
        if (sessionEmitters != null) {
            sessionEmitters.forEach(SseEmitter::complete);
        }
    }

    public int subscriberCount(String sdkSessionId) {
        var set = emitters.get(sdkSessionId);
        return set == null ? 0 : set.size();
    }
}
