package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.FollowEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.FollowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FollowService {

    private static final Logger logger = LoggerFactory.getLogger(FollowService.class);

    private final FollowRepository followRepository;
    private final UserService userService;

    public FollowService(FollowRepository followRepository, UserService userService) {
        this.followRepository = followRepository;
        this.userService = userService;
    }

    @Transactional
    public FollowEntity follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Não é possível seguir a si mesmo");
        }

        UserEntity follower = userService.findById(followerId);
        UserEntity following = userService.findById(followingId);

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new IllegalStateException("Você já segue este usuário");
        }

        FollowEntity follow = new FollowEntity();
        follow.setFollower(follower);
        follow.setFollowing(following);

        return followRepository.save(follow);
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new IllegalArgumentException("Você não segue este usuário");
        }
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    public List<UserEntity> getFollowing(Long userId) {
        userService.findById(userId); // Validate user existence
        return followRepository.findByFollowerId(userId).stream()
                .map(FollowEntity::getFollowing)
                .collect(Collectors.toList());
    }

    public List<UserEntity> getFollowers(Long userId) {
        userService.findById(userId); // Validate user existence
        return followRepository.findByFollowingId(userId).stream()
                .map(FollowEntity::getFollower)
                .collect(Collectors.toList());
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public long countFollowing(Long userId) {
        return followRepository.countFollowing(userId);
    }

    public long countFollowers(Long userId) {
        return followRepository.countFollowers(userId);
    }
}
