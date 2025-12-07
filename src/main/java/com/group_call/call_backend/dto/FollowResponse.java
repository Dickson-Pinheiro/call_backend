package com.group_call.call_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponse {
    private Long id;
    private UserResponse follower;
    private UserResponse following;
    private LocalDateTime followedAt;
}
