package com.group_call.call_backend.controller;

import com.group_call.call_backend.dto.CallRatingCreateRequest;
import com.group_call.call_backend.dto.CallRatingResponse;
import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.CallRatingEntity;
import com.group_call.call_backend.security.AuthenticationHelper;
import com.group_call.call_backend.service.CallRatingService;
import com.group_call.call_backend.service.CallService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ratings")
public class CallRatingController {

    private final CallRatingService callRatingService;
    private final CallService callService;
    private final AuthenticationHelper authHelper;

    public CallRatingController(CallRatingService callRatingService,
                                CallService callService,
                                AuthenticationHelper authHelper) {
        this.callRatingService = callRatingService;
        this.callService = callService;
        this.authHelper = authHelper;
    }

    @PostMapping
    public ResponseEntity<CallRatingResponse> createRating(@Valid @RequestBody CallRatingCreateRequest request) {
        Long currentUserId = authHelper.getCurrentUserId();
        
        if (!currentUserId.equals(request.getRaterId())) {
            throw new IllegalArgumentException("Você só pode criar avaliações em seu próprio nome");
        }
        
        CallEntity call = callService.findById(request.getCallId());
        if (!authHelper.isInCall(call.getUser1().getId(), call.getUser2().getId())) {
            throw new IllegalArgumentException("Você só pode avaliar chamadas que participou");
        }
        
        CallRatingEntity rating = callRatingService.createRating(
                request.getCallId(),
                request.getRaterId(),
                request.getRating(),
                request.getComment()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(rating));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CallRatingResponse> getRatingById(@PathVariable Long id) {
        CallRatingEntity rating = callRatingService.findById(id);
        
        CallEntity call = rating.getCall();
        if (!authHelper.isInCall(call.getUser1().getId(), call.getUser2().getId())) {
            throw new IllegalArgumentException("Acesso negado");
        }
        
        return ResponseEntity.ok(toResponse(rating));
    }

    @GetMapping
    public ResponseEntity<List<CallRatingResponse>> getAllRatings() {
        Long currentUserId = authHelper.getCurrentUserId();
        
        List<CallRatingEntity> ratings = callRatingService.getAllRatings().stream()
                .filter(rating -> {
                    CallEntity call = rating.getCall();
                    return call.getUser1().getId().equals(currentUserId) || 
                           call.getUser2().getId().equals(currentUserId);
                })
                .collect(Collectors.toList());
        
        List<CallRatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/min-rating/{rating}")
    public ResponseEntity<List<CallRatingResponse>> getRatingsByMinimum(@PathVariable Integer rating) {
        Long currentUserId = authHelper.getCurrentUserId();
        
        List<CallRatingEntity> ratings = callRatingService.findByRatingGreaterThanEqual(rating).stream()
                .filter(r -> {
                    CallEntity call = r.getCall();
                    return call.getUser1().getId().equals(currentUserId) || 
                           call.getUser2().getId().equals(currentUserId);
                })
                .collect(Collectors.toList());
        
        List<CallRatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top")
    public ResponseEntity<List<CallRatingResponse>> getTopRatings() {
        Long currentUserId = authHelper.getCurrentUserId();

        List<CallRatingEntity> ratings = callRatingService.findTopRatings().stream()
                .filter(r -> {
                    CallEntity call = r.getCall();
                    return call.getUser1().getId().equals(currentUserId) || 
                           call.getUser2().getId().equals(currentUserId);
                })
                .collect(Collectors.toList());
        
        List<CallRatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/positive")
    public ResponseEntity<List<CallRatingResponse>> getPositiveRatings() {
        Long currentUserId = authHelper.getCurrentUserId();
        
        List<CallRatingEntity> ratings = callRatingService.findPositiveRatings().stream()
                .filter(r -> {
                    CallEntity call = r.getCall();
                    return call.getUser1().getId().equals(currentUserId) || 
                           call.getUser2().getId().equals(currentUserId);
                })
                .collect(Collectors.toList());
        
        List<CallRatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CallRatingResponse> updateRating(
            @PathVariable Long id,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String comment) {
        
        CallRatingEntity existingRating = callRatingService.findById(id);
        
        if (!authHelper.isOwner(existingRating.getRater().getId())) {
            throw new IllegalArgumentException("Você só pode atualizar suas próprias avaliações");
        }
        
        CallRatingEntity updatedRating = callRatingService.updateRating(id, rating, comment);
        return ResponseEntity.ok(toResponse(updatedRating));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRating(@PathVariable Long id) {
        CallRatingEntity rating = callRatingService.findById(id);
        
        if (!authHelper.isOwner(rating.getRater().getId())) {
            throw new IllegalArgumentException("Você só pode deletar suas próprias avaliações");
        }
        
        callRatingService.deleteRating(id);
        return ResponseEntity.noContent().build();
    }

    private CallRatingResponse toResponse(CallRatingEntity rating) {
        CallRatingResponse response = new CallRatingResponse();
        response.setId(rating.getId());
        response.setCallId(rating.getCall().getId());
        response.setRaterId(rating.getRater().getId());
        response.setRaterName(rating.getRater().getName());
        response.setRating(rating.getRating());
        response.setComment(rating.getComment());
        response.setCreatedAt(rating.getCreatedAt());
        return response;
    }
}
