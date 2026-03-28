package com.elzakaria.copiweb.dto;

public record EventDto(
    String type,
    String content,
    String toolName,
    String args,
    String result,
    String sessionId,
    long timestamp
) {
    public static EventDto delta(String chunk, String sessionId) {
        return new EventDto("ASSISTANT_DELTA", chunk, null, null, null, sessionId, System.currentTimeMillis());
    }

    public static EventDto assistantMessage(String content, String sessionId) {
        return new EventDto("ASSISTANT_MSG", content, null, null, null, sessionId, System.currentTimeMillis());
    }

    public static EventDto idle(String sessionId) {
        return new EventDto("IDLE", null, null, null, null, sessionId, System.currentTimeMillis());
    }

    public static EventDto toolStart(String toolName, String args, String sessionId) {
        return new EventDto("TOOL_START", null, toolName, args, null, sessionId, System.currentTimeMillis());
    }

    public static EventDto toolComplete(String toolName, String result, String sessionId) {
        return new EventDto("TOOL_COMPLETE", null, toolName, null, result, sessionId, System.currentTimeMillis());
    }

    public static EventDto subagentStart(String sessionId) {
        return new EventDto("SUBAGENT_START", null, null, null, null, sessionId, System.currentTimeMillis());
    }

    public static EventDto subagentComplete(String sessionId) {
        return new EventDto("SUBAGENT_COMPLETE", null, null, null, null, sessionId, System.currentTimeMillis());
    }

    public static EventDto error(String message, String sessionId) {
        return new EventDto("SESSION_ERROR", message, null, null, null, sessionId, System.currentTimeMillis());
    }

    public static EventDto abort(String sessionId) {
        return new EventDto("ABORT", null, null, null, null, sessionId, System.currentTimeMillis());
    }
}
