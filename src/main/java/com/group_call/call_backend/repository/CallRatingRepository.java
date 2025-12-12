package com.group_call.call_backend.repository;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.CallRatingEntity;
import com.group_call.call_backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallRatingRepository extends JpaRepository<CallRatingEntity, Long> {

    List<CallRatingEntity> findByCall(CallEntity call);

    boolean existsByCallAndRater(CallEntity call, UserEntity rater);

    List<CallRatingEntity> findByRater(UserEntity rater);

    boolean existsByCall(CallEntity call);

    @Query("SELECT AVG(cr.rating) FROM CallRatingEntity cr WHERE cr.call.user1 = :user OR cr.call.user2 = :user")
    Double getAverageRatingForUser(@Param("user") UserEntity user);

    List<CallRatingEntity> findByRatingGreaterThanEqual(Integer rating);

    @Query("SELECT cr FROM CallRatingEntity cr JOIN FETCH cr.rater JOIN FETCH cr.call c JOIN FETCH c.user1 JOIN FETCH c.user2")
    List<CallRatingEntity> findAllWithDetails();

    @Query("SELECT cr FROM CallRatingEntity cr JOIN FETCH cr.rater JOIN FETCH cr.call c JOIN FETCH c.user1 JOIN FETCH c.user2 WHERE cr.id = :id")
    Optional<CallRatingEntity> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT cr FROM CallRatingEntity cr JOIN FETCH cr.rater JOIN FETCH cr.call c JOIN FETCH c.user1 JOIN FETCH c.user2 WHERE cr.call = :call")
    List<CallRatingEntity> findByCallWithDetails(@Param("call") CallEntity call);

    @Query("SELECT cr FROM CallRatingEntity cr JOIN FETCH cr.rater JOIN FETCH cr.call c JOIN FETCH c.user1 JOIN FETCH c.user2 WHERE cr.rater = :rater")
    List<CallRatingEntity> findByRaterWithDetails(@Param("rater") UserEntity rater);
}
