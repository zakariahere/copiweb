package com.elzakaria.copiweb.model;

public enum EventType {
    USER_MSG,
    ASSISTANT_MSG,
    ASSISTANT_DELTA,
    TOOL_START,
    TOOL_COMPLETE,
    SUBAGENT_START,
    SUBAGENT_COMPLETE,
    A2A_SEND,
    A2A_RECEIVE,
    SESSION_ERROR,
    IDLE,
    ABORT
}
