package com.group_call.call_backend.websocket.message;

public enum WebSocketMessageType {
    CHAT_MESSAGE("chat"),
    TYPING_INDICATOR("typing"),
    WEBRTC_SIGNAL("webrtc"),
    MATCH_FOUND("match"),
    CALL_ENDED("call_ended"),
    ERROR("error");

    private final String type;

    WebSocketMessageType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
