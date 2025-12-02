package com.group_call.call_backend.dto;

import lombok.Data;

@Data
public class WebRTCSignal {
    private String type;
    private Long callId;
    private Long targetUserId;
    private Object data;
}
