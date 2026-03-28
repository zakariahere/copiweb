package com.elzakaria.copiweb.agent;

import com.github.copilot.sdk.CopilotSession;

import java.util.concurrent.atomic.AtomicInteger;

public record CopilotSessionHandle(
    CopilotSession sdkSession,
    String sdkSessionId,
    Long dbSessionId,
    AtomicInteger sequenceCounter
) {
    public int nextSequence() {
        return sequenceCounter.getAndIncrement();
    }
}
