package com.group_call.call_backend.controller;

import com.group_call.call_backend.dto.CallRatingCreateRequest;
import com.group_call.call_backend.dto.CallRatingResponse;
import com.group_call.call_backend.entity.CallRatingEntity;
import com.group_call.call_backend.service.CallRatingService;
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

    public CallRatingController(CallRatingService callRatingService) {
        this.callRatingService = callRatingService;
    }

    @PostMapping
    public ResponseEntity<CallRatingResponse> createRating(@Valid @RequestBody CallRatingCreateRequest request) {
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
        return ResponseEntity.ok(toResponse(rating));
    }

    @GetMapping
    public ResponseEntity<List<CallRatingResponse>> getAllRatings() {
        List<CallRatingEntity> ratings = callRatingService.getAllRatings();
        List<CallRatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/min-rating/{rating}")
    public ResponseEntity<List<CallRatingResponse>> getRatingsByMinimum(@PathVariable Integer rating) {
        List<CallRatingEntity> ratings = callRatingService.findByRatingGreaterThanEqual(rating);
        List<CallRatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top")
    public ResponseEntity<List<CallRatingResponse>> getTopRatings() {
        List<CallRatingEntity> ratings = callRatingService.findTopRatings();
        List<CallRatingResponse> response = ratings.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/positive")
    public ResponseEntity<List<CallRatingResponse>> getPositiveRatings() {
        List<CallRatingEntity> ratings = callRatingService.findPositiveRatings();
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
        
        CallRatingEntity updatedRating = callRatingService.updateRating(id, rating, comment);
        return ResponseEntity.ok(toResponse(updatedRating));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRating(@PathVariable Long id) {
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
