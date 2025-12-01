package com.group_call.call_backend.repository;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CallRepository extends JpaRepository<CallEntity, Long> {

    List<CallEntity> findByUser1OrUser2(UserEntity user1, UserEntity user2);

    List<CallEntity> findByStatus(CallEntity.CallStatus status);

    @Query("SELECT c FROM CallEntity c WHERE (c.user1 = :user OR c.user2 = :user) AND c.status = :status")
    List<CallEntity> findByUserAndStatus(@Param("user") UserEntity user, @Param("status") CallEntity.CallStatus status);

    @Query("SELECT c FROM CallEntity c WHERE (c.user1 = :user OR c.user2 = :user) AND c.startedAt BETWEEN :startDate AND :endDate")
    List<CallEntity> findByUserAndDateRange(@Param("user") UserEntity user, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
