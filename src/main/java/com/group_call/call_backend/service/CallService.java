package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.CallRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CallService {

    private final UserService userService;
    private final CallRepository callRepository;

    public CallService(UserService userService, CallRepository callRepository) {
        this.userService = userService;
        this.callRepository = callRepository;
    }

    public CallEntity createCall(Long user1Id, Long user2Id, CallEntity.CallType callType) {
        UserEntity user1 = userService.findById(user1Id);
        UserEntity user2 = userService.findById(user2Id);

        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("Não é possível criar uma chamada consigo mesmo");
        }

        CallEntity call = new CallEntity();
        call.setUser1(user1);
        call.setUser2(user2);
        call.setCallType(callType != null ? callType : CallEntity.CallType.VIDEO);
        call.setStatus(CallEntity.CallStatus.ACTIVE);
        call.setStartedAt(LocalDateTime.now());

        return callRepository.save(call);
    }

    public CallEntity findById(Long id) {
        return callRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chamada não encontrada com ID: " + id));
    }

    public List<CallEntity> getAllCalls() {
        return callRepository.findAllWithUsers();
    }

    public List<CallEntity> findByStatus(CallEntity.CallStatus status) {
        return callRepository.findByStatusWithUsers(status);
    }

    public CallEntity endCall(Long callId) {
        CallEntity call = findById(callId);

        if (!call.getStatus().equals(CallEntity.CallStatus.ACTIVE)) {
            throw new IllegalStateException("Chamada já foi finalizada");
        }

        call.setEndedAt(LocalDateTime.now());
        call.setStatus(CallEntity.CallStatus.COMPLETED);

        if (call.getStartedAt() != null && call.getEndedAt() != null) {
            Duration duration = Duration.between(call.getStartedAt(), call.getEndedAt());
            call.setDurationSeconds((int) duration.getSeconds());
        }

        return callRepository.save(call);
    }

    public CallEntity cancelCall(Long callId) {
        CallEntity call = findById(callId);

        if (!call.getStatus().equals(CallEntity.CallStatus.ACTIVE)) {
            throw new IllegalStateException("Chamada já foi finalizada");
        }

        call.setEndedAt(LocalDateTime.now());
        call.setStatus(CallEntity.CallStatus.CANCELLED);

        if (call.getStartedAt() != null && call.getEndedAt() != null) {
            Duration duration = Duration.between(call.getStartedAt(), call.getEndedAt());
            call.setDurationSeconds((int) duration.getSeconds());
        }

        return callRepository.save(call);
    }

    public CallEntity updateCallType(Long callId, CallEntity.CallType callType) {
        CallEntity call = findById(callId);
        call.setCallType(callType);
        return callRepository.save(call);
    }

    public void deleteCall(Long callId) {
        if (!callRepository.existsById(callId)) {
            throw new IllegalArgumentException("Chamada não encontrada com ID: " + callId);
        }
        callRepository.deleteById(callId);
    }

    public List<CallEntity> getActiveCalls() {
        return findByStatus(CallEntity.CallStatus.ACTIVE);
    }

    public List<CallEntity> getCompletedCalls() {
        return findByStatus(CallEntity.CallStatus.COMPLETED);
    }
}
