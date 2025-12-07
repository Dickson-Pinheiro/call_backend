package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.tree.CallTree;
import com.group_call.call_backend.repository.CallRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CallService {

    private final CallTree callTree;
    private final UserService userService;
    private final CallRepository callRepository;

    public CallService(CallTree callTree, UserService userService, CallRepository callRepository) {
        this.callTree = callTree;
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

        return callTree.addCall(call);
    }

    public CallEntity findById(Long id) {
        // Primeiro tenta na árvore em memória (rápido)
        CallEntity call = callTree.findById(id);
        if (call != null) {
            return call;
        }

        // Se não estiver na árvore, tenta no repositório como fallback
        return callRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chamada não encontrada com ID: " + id));
    }

    public List<CallEntity> getAllCalls() {
        return callTree.getAllCallsSorted();
    }

    public List<CallEntity> findByStatus(CallEntity.CallStatus status) {
        return callTree.findByStatus(status);
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

        return callTree.updateCall(call);
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

        return callTree.updateCall(call);
    }

    public CallEntity updateCallType(Long callId, CallEntity.CallType callType) {
        CallEntity call = findById(callId);
        call.setCallType(callType);
        return callTree.updateCall(call);
    }

    public void deleteCall(Long callId) {
        findById(callId);
        callTree.removeCall(callId);
    }

    public List<CallEntity> getActiveCalls() {
        return findByStatus(CallEntity.CallStatus.ACTIVE);
    }

    public List<CallEntity> getCompletedCalls() {
        return findByStatus(CallEntity.CallStatus.COMPLETED);
    }
    
    public void reloadTree() {
        callTree.reload();
    }
}
