package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.FollowEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.FollowRepository;
import com.group_call.call_backend.tree.FollowTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FollowService {

    private static final Logger logger = LoggerFactory.getLogger(FollowService.class);

    private final FollowTree followTree;
    private final FollowRepository followRepository;
    private final UserService userService;
    private final RedisFollowSyncService redisFollowSync;

    public FollowService(FollowTree followTree, FollowRepository followRepository,
                        UserService userService, RedisFollowSyncService redisFollowSync) {
        this.followTree = followTree;
        this.followRepository = followRepository;
        this.userService = userService;
        this.redisFollowSync = redisFollowSync;
    }

    @Transactional
    public FollowEntity follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Não é possível seguir a si mesmo");
        }

        UserEntity follower = userService.findById(followerId);
        UserEntity following = userService.findById(followingId);

        FollowEntity existingFollow = followTree.findByFollowerAndFollowing(followerId, followingId);
        if (existingFollow != null) {
            throw new IllegalStateException("Você já segue este usuário");
        }

        FollowEntity follow = new FollowEntity();
        follow.setFollower(follower);
        follow.setFollowing(following);

        FollowEntity savedFollow = followTree.addFollow(follow);
        
        redisFollowSync.publishFollowAction("ADD", savedFollow.getId(), followerId, followingId);

        return savedFollow;
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        FollowEntity follow = followTree.findByFollowerAndFollowing(followerId, followingId);
        if (follow == null) {
            throw new IllegalArgumentException("Você não segue este usuário");
        }

        Long followId = follow.getId();
        followTree.removeFollow(followId);
        
        redisFollowSync.publishFollowAction("REMOVE", followId, followerId, followingId);
    }

    public List<UserEntity> getFollowing(Long userId) {
        userService.findById(userId);
        List<FollowEntity> follows = followTree.findByFollowerId(userId);
        return follows.stream()
                .map(FollowEntity::getFollowing)
                .collect(Collectors.toList());
    }

    public List<UserEntity> getFollowers(Long userId) {
        userService.findById(userId);
        List<FollowEntity> follows = followTree.findByFollowingId(userId);
        return follows.stream()
                .map(FollowEntity::getFollower)
                .collect(Collectors.toList());
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followTree.findByFollowerAndFollowing(followerId, followingId) != null;
    }

    public long countFollowing(Long userId) {
        return followTree.findByFollowerId(userId).size();
    }

    public long countFollowers(Long userId) {
        return followTree.findByFollowingId(userId).size();
    }

    public void reloadTree() {
        followTree.reload();
    }

    public void syncFollowAction(String action, Long followId, Long followerId, Long followingId) {
        try {
            if ("ADD".equals(action)) {
                FollowEntity follow = followRepository.findById(followId).orElse(null);
                if (follow != null && followTree.findById(followId) == null) {
                    followTree.insert(follow.getId(), follow);
                }
            } else if ("REMOVE".equals(action)) {
                if (followTree.findById(followId) != null) {
                    followTree.delete(followId);
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao sincronizar follow action={}, followId={}", action, followId, e);
        }
    }
}
