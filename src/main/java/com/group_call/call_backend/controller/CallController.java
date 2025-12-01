package com.group_call.call_backend.controller;

import com.group_call.call_backend.dto.CallCreateRequest;
import com.group_call.call_backend.dto.CallResponse;
import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.service.CallService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/calls")
public class CallController {

    private final CallService callService;

    public CallController(CallService callService) {
        this.callService = callService;
    }

    @PostMapping
    public ResponseEntity<CallResponse> createCall(@Valid @RequestBody CallCreateRequest request) {
        CallEntity.CallType callType = CallEntity.CallType.VIDEO;
        if (request.getCallType() != null) {
            try {
                callType = CallEntity.CallType.valueOf(request.getCallType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo de chamada inválido. Use VIDEO ou AUDIO");
            }
        }

        CallEntity call = callService.createCall(
                request.getUser1Id(),
                request.getUser2Id(),
                callType
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(call));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CallResponse> getCallById(@PathVariable Long id) {
        CallEntity call = callService.findById(id);
        return ResponseEntity.ok(toResponse(call));
    }

    @GetMapping
    public ResponseEntity<List<CallResponse>> getAllCalls() {
        List<CallEntity> calls = callService.getAllCalls();
        List<CallResponse> response = calls.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CallResponse>> getCallsByStatus(@PathVariable String status) {
        try {
            CallEntity.CallStatus callStatus = CallEntity.CallStatus.valueOf(status.toUpperCase());
            List<CallEntity> calls = callService.findByStatus(callStatus);
            List<CallResponse> response = calls.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido. Use ACTIVE, COMPLETED ou CANCELLED");
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<CallResponse>> getActiveCalls() {
        List<CallEntity> calls = callService.getActiveCalls();
        List<CallResponse> response = calls.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/completed")
    public ResponseEntity<List<CallResponse>> getCompletedCalls() {
        List<CallEntity> calls = callService.getCompletedCalls();
        List<CallResponse> response = calls.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<CallResponse> endCall(@PathVariable Long id) {
        CallEntity call = callService.endCall(id);
        return ResponseEntity.ok(toResponse(call));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CallResponse> cancelCall(@PathVariable Long id) {
        CallEntity call = callService.cancelCall(id);
        return ResponseEntity.ok(toResponse(call));
    }

    @PatchMapping("/{id}/type")
    public ResponseEntity<CallResponse> updateCallType(
            @PathVariable Long id,
            @RequestParam String callType) {
        
        try {
            CallEntity.CallType type = CallEntity.CallType.valueOf(callType.toUpperCase());
            CallEntity call = callService.updateCallType(id, type);
            return ResponseEntity.ok(toResponse(call));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de chamada inválido. Use VIDEO ou AUDIO");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCall(@PathVariable Long id) {
        callService.deleteCall(id);
        return ResponseEntity.noContent().build();
    }

    private CallResponse toResponse(CallEntity call) {
        CallResponse response = new CallResponse();
        response.setId(call.getId());
        response.setUser1Id(call.getUser1().getId());
        response.setUser1Name(call.getUser1().getName());
        response.setUser2Id(call.getUser2().getId());
        response.setUser2Name(call.getUser2().getName());
        response.setStartedAt(call.getStartedAt());
        response.setEndedAt(call.getEndedAt());
        response.setDurationSeconds(call.getDurationSeconds());
        response.setCallType(call.getCallType().name());
        response.setStatus(call.getStatus().name());
        return response;
    }
}
