package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.CallRatingEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.CallRatingRepository;
import com.group_call.call_backend.tree.CallRatingTree;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CallRatingService {

    private final CallRatingTree callRatingTree;
    private final CallRatingRepository callRatingRepository;
    private final CallService callService;
    private final UserService userService;

    public CallRatingService(CallRatingTree callRatingTree, CallRatingRepository callRatingRepository,
                           CallService callService, UserService userService) {
        this.callRatingTree = callRatingTree;
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

        CallRatingEntity callRating = new CallRatingEntity();
        callRating.setCall(call);
        callRating.setRater(rater);
        callRating.setRating(rating);
        callRating.setComment(comment);

        return callRatingTree.addRating(callRating);
    }

    public CallRatingEntity findById(Long id) {
        CallRatingEntity rating = callRatingTree.findById(id);
        if (rating == null) {
            // Fallback: tenta buscar no repository
            rating = callRatingRepository.findById(id).orElse(null);
        }
        if (rating == null) {
            throw new IllegalArgumentException("Avaliação não encontrada com ID: " + id);
        }
        return rating;
    }

    public List<CallRatingEntity> getAllRatings() {
        return callRatingTree.getAllRatingsSorted();
    }

    public List<CallRatingEntity> findByRatingGreaterThanEqual(Integer rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating deve ser entre 1 e 5");
        }
        return callRatingTree.findByRatingGreaterThanEqual(rating);
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

        return callRatingTree.updateRating(rating);
    }

    public void deleteRating(Long ratingId) {
        findById(ratingId); // Valida se existe
        callRatingTree.removeRating(ratingId);
    }

    public List<CallRatingEntity> findTopRatings() {
        return findByRatingGreaterThanEqual(5);
    }

    public List<CallRatingEntity> findPositiveRatings() {
        return findByRatingGreaterThanEqual(4);
    }

    public void reloadTree() {
        callRatingTree.reload();
    }
}
