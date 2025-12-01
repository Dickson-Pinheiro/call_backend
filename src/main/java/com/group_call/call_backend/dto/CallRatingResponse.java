package com.group_call.call_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallRatingResponse {
    private Long id;
    private Long callId;
    private Long raterId;
    private String raterName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
