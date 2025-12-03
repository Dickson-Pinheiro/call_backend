package com.group_call.call_backend.tree;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.repository.CallRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CallTree extends AVLTree<CallEntity> {

    private final CallRepository callRepository;

    public CallTree(CallRepository callRepository) {
        this.callRepository = callRepository;
        loadFromDatabase();
    }

    public void loadFromDatabase() {
        clear();
        List<CallEntity> calls = callRepository.findAllWithUsers();
        for (CallEntity call : calls) {
            insert(call.getId(), call);
        }
    }

    public CallEntity addCall(CallEntity call) {
        CallEntity savedCall = callRepository.save(call);
        insert(savedCall.getId(), savedCall);
        return savedCall;
    }

    public CallEntity updateCall(CallEntity call) {
        CallEntity updatedCall = callRepository.save(call);
        delete(updatedCall.getId());
        insert(updatedCall.getId(), updatedCall);
        return updatedCall;
    }

    public void removeCall(Long callId) {
        callRepository.deleteById(callId);
        delete(callId);
    }

    public CallEntity findById(Long id) {
        return search(id);
    }

    public List<CallEntity> findByStatus(CallEntity.CallStatus status) {
        return callRepository.findByStatusWithUsers(status);
    }

    public List<CallEntity> getAllCallsSorted() {
        return inOrderTraversal();
    }

    public void reload() {
        loadFromDatabase();
    }
}
