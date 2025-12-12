package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.CallRatingEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.CallRatingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CallRatingService {

    private final CallRatingRepository callRatingRepository;
    private final CallService callService;
    private final UserService userService;

    public CallRatingService(CallRatingRepository callRatingRepository,
            CallService callService, UserService userService) {
        this.callRatingRepository = callRatingRepository;
        this.callService = callService;
        this.userService = userService;
    }

    public CallRatingEntity createRating(Long callId, Long raterId, Integer rating, String comment) {
        CallEntity call = callService.findById(callId);
        UserEntity rater = userService.findById(raterId);

        if (!call.getUser1().getId().equals(raterId) && !call.getUser2().getId().equals(raterId)) {
            throw new IllegalArgumentException("Usuário não faz parte desta chamada");
        }

        if (call.getStatus().equals(CallEntity.CallStatus.ACTIVE)) {
            throw new IllegalStateException("Não é possível avaliar chamadas em andamento");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Avaliação deve ser entre 1 e 5");
        }

        if (callRatingRepository.existsByCallAndRater(call, rater)) {
            throw new IllegalStateException("Você já avaliou esta chamada");
        }

        CallRatingEntity callRating = new CallRatingEntity();
        callRating.setCall(call);
        callRating.setRater(rater);
        callRating.setRating(rating);
        callRating.setComment(comment);

        return callRatingRepository.save(callRating);
    }

    public CallRatingEntity findById(Long id) {
        return callRatingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Avaliação não encontrada com ID: " + id));
    }

    public List<CallRatingEntity> getAllRatings() {
        return callRatingRepository.findAllWithDetails();
    }

    public List<CallRatingEntity> findByRatingGreaterThanEqual(Integer rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating deve ser entre 1 e 5");
        }
        return callRatingRepository.findByRatingGreaterThanEqual(rating);
    }

    public CallRatingEntity updateRating(Long ratingId, Integer newRating, String newComment) {
        CallRatingEntity rating = findById(ratingId);

        if (newRating != null) {
            if (newRating < 1 || newRating > 5) {
                throw new IllegalArgumentException("Avaliação deve ser entre 1 e 5");
            }
            rating.setRating(newRating);
        }

        if (newComment != null) {
            rating.setComment(newComment);
        }

        return callRatingRepository.save(rating);
    }

    public void deleteRating(Long ratingId) {
        if (!callRatingRepository.existsById(ratingId)) {
            throw new IllegalArgumentException("Avaliação não encontrada com ID: " + ratingId);
        }
        callRatingRepository.deleteById(ratingId);
    }

    public List<CallRatingEntity> findTopRatings() {
        return findByRatingGreaterThanEqual(5);
    }

    public List<CallRatingEntity> findPositiveRatings() {
        return findByRatingGreaterThanEqual(4);
    }
}
