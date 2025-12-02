package com.group_call.call_backend.dto;

import lombok.Data;

@Data
public class ChatMessage {
    private Long callId;
    private String message;
}
