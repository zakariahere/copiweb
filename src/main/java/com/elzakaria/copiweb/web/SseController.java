package com.elzakaria.copiweb.web;

import com.elzakaria.copiweb.service.SseService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private static final long DEMO_STREAM_TIMEOUT_MILLIS = 86_400_000L;

    private final SseService sseService;
    private final ExecutorService demoStreamExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @GetMapping(value = "/{sdkSessionId}/stream", produces = "text/event-stream")
    public SseEmitter stream(@PathVariable String sdkSessionId) {
        log.debug("SSE subscription for sdkSessionId={}", sdkSessionId);
        return sseService.subscribe(sdkSessionId);
    }

    @GetMapping(value = "/stream2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream2() {
        var emitter = new SseEmitter(DEMO_STREAM_TIMEOUT_MILLIS);

        demoStreamExecutor.submit(() -> {
            int count = 1;
            try {
                emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("stream2 connected"));

                while (true) {
                    emitter.send(SseEmitter.event()
                        .id(String.valueOf(count))
                        .name("hello")
                        .data("hello world " + count));
                    count++;
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (IOException | IllegalStateException e) {
                log.debug("stream2 client disconnected: {}", e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("stream2 worker interrupted");
            } finally {
                emitter.complete();
            }
        });

        return emitter;
    }

    @PreDestroy
    void shutdownDemoStreamExecutor() {
        demoStreamExecutor.close();
    }
}
