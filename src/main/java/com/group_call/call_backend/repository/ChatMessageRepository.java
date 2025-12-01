package com.group_call.call_backend.repository;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.ChatMessageEntity;
import com.group_call.call_backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findByCallOrderBySentAtAsc(CallEntity call);

    List<ChatMessageEntity> findBySender(UserEntity sender);

    List<ChatMessageEntity> findByCallIdOrderBySentAtAsc(Long callId);
}
