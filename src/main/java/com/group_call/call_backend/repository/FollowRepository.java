package com.group_call.call_backend.repository;

import com.group_call.call_backend.entity.FollowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, Long> {

    Optional<FollowEntity> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<FollowEntity> findByFollowerId(Long followerId);

    List<FollowEntity> findByFollowingId(Long followingId);

    @Query("SELECT COUNT(f) FROM FollowEntity f WHERE f.follower.id = :userId")
    long countFollowing(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM FollowEntity f WHERE f.following.id = :userId")
    long countFollowers(@Param("userId") Long userId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
