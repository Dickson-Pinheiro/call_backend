package com.group_call.call_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebRTCSignal {
    private String type;
    private Long callId;
    private Long targetUserId;
    private Object data;
}
