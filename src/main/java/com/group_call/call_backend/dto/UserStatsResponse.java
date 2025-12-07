package com.group_call.call_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {
    private Long userId;
    private String name;
    private String email;
    private long followingCount;
    private long followersCount;
    private Boolean isFollowing;
}
