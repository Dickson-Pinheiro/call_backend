package com.group_call.call_backend.controller;

import com.group_call.call_backend.dto.UserResponse;
import com.group_call.call_backend.dto.UserStatsResponse;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.service.FollowService;
import com.group_call.call_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;
    private final UserService userService;

    public FollowController(FollowService followService, UserService userService) {
        this.followService = followService;
        this.userService = userService;
    }

    @PostMapping("/{followingId}")
    public ResponseEntity<Map<String, Object>> follow(
            @PathVariable Long followingId,
            @RequestParam Long userId) {
        followService.follow(userId, followingId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Você agora está seguindo este usuário");
        response.put("followerId", userId);
        response.put("followingId", followingId);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{followingId}")
    public ResponseEntity<Map<String, Object>> unfollow(
            @PathVariable Long followingId,
            @RequestParam Long userId) {
        followService.unfollow(userId, followingId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Você deixou de seguir este usuário");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<List<UserResponse>> getFollowing(@PathVariable Long userId) {
        List<UserEntity> following = followService.getFollowing(userId);
        List<UserResponse> response = following.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<UserResponse>> getFollowers(@PathVariable Long userId) {
        List<UserEntity> followers = followService.getFollowers(userId);
        List<UserResponse> response = followers.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(
            @PathVariable Long userId,
            @RequestParam(required = false) Long currentUserId) {
        UserEntity user = userService.findById(userId);
        
        UserStatsResponse stats = new UserStatsResponse();
        stats.setUserId(user.getId());
        stats.setName(user.getName());
        stats.setEmail(user.getEmail());
        stats.setFollowingCount(followService.countFollowing(userId));
        stats.setFollowersCount(followService.countFollowers(userId));
        
        if (currentUserId != null) {
            stats.setIsFollowing(followService.isFollowing(currentUserId, userId));
        } else {
            stats.setIsFollowing(false);
        }
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkFollowing(
            @RequestParam Long followerId,
            @RequestParam Long followingId) {
        boolean isFollowing = followService.isFollowing(followerId, followingId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("isFollowing", isFollowing);
        
        return ResponseEntity.ok(response);
    }

    private UserResponse convertToUserResponse(UserEntity user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setIsActive(user.getIsActive());
        response.setIsOnline(user.getIsOnline());
        return response;
    }
}
