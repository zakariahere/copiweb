package com.elzakaria.copiweb.web;

import com.elzakaria.copiweb.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final SseService sseService;

    @GetMapping(value = "/{sdkSessionId}/stream", produces = "text/event-stream")
    public SseEmitter stream(@PathVariable String sdkSessionId) {
        log.debug("SSE subscription for sdkSessionId={}", sdkSessionId);
        return sseService.subscribe(sdkSessionId);
    }
}
