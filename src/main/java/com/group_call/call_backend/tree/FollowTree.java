package com.group_call.call_backend.tree;

import com.group_call.call_backend.entity.FollowEntity;
import com.group_call.call_backend.repository.FollowRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FollowTree extends AVLTree<FollowEntity> {

    private final FollowRepository followRepository;

    public FollowTree(FollowRepository followRepository) {
        this.followRepository = followRepository;
        loadFromDatabase();
    }

    public void loadFromDatabase() {
        clear();
        List<FollowEntity> follows = followRepository.findAll();
        for (FollowEntity follow : follows) {
            insert(follow.getId(), follow);
        }
    }

    public FollowEntity addFollow(FollowEntity follow) {
        FollowEntity savedFollow = followRepository.save(follow);
        insert(savedFollow.getId(), savedFollow);
        return savedFollow;
    }

    public void removeFollow(Long followId) {
        followRepository.deleteById(followId);
        delete(followId);
    }

    public FollowEntity findById(Long id) {
        return search(id);
    }

    public List<FollowEntity> findByFollowerId(Long followerId) {
        List<FollowEntity> allFollows = inOrderTraversal();
        List<FollowEntity> result = new ArrayList<>();
        for (FollowEntity follow : allFollows) {
            if (follow.getFollower().getId().equals(followerId)) {
                result.add(follow);
            }
        }
        return result;
    }

    public List<FollowEntity> findByFollowingId(Long followingId) {
        List<FollowEntity> allFollows = inOrderTraversal();
        List<FollowEntity> result = new ArrayList<>();
        for (FollowEntity follow : allFollows) {
            if (follow.getFollowing().getId().equals(followingId)) {
                result.add(follow);
            }
        }
        return result;
    }

    public FollowEntity findByFollowerAndFollowing(Long followerId, Long followingId) {
        List<FollowEntity> allFollows = inOrderTraversal();
        for (FollowEntity follow : allFollows) {
            if (follow.getFollower().getId().equals(followerId) 
                && follow.getFollowing().getId().equals(followingId)) {
                return follow;
            }
        }
        return null;
    }

    public List<FollowEntity> getAllFollowsSorted() {
        return inOrderTraversal();
    }

    public void reload() {
        loadFromDatabase();
    }
}
