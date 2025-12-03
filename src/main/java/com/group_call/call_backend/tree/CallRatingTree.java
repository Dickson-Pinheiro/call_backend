package com.group_call.call_backend.tree;

import com.group_call.call_backend.entity.CallRatingEntity;
import com.group_call.call_backend.repository.CallRatingRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CallRatingTree extends AVLTree<CallRatingEntity> {

    private final CallRatingRepository callRatingRepository;

    public CallRatingTree(CallRatingRepository callRatingRepository) {
        this.callRatingRepository = callRatingRepository;
        loadFromDatabase();
    }

    public void loadFromDatabase() {
        clear();
        List<CallRatingEntity> ratings = callRatingRepository.findAllWithDetails();
        for (CallRatingEntity rating : ratings) {
            insert(rating.getId(), rating);
        }
    }

    public CallRatingEntity addRating(CallRatingEntity rating) {
        CallRatingEntity savedRating = callRatingRepository.save(rating);
        CallRatingEntity ratingWithDetails = callRatingRepository.findByIdWithDetails(savedRating.getId())
                .orElse(savedRating);
        insert(ratingWithDetails.getId(), ratingWithDetails);
        return ratingWithDetails;
    }

    public CallRatingEntity updateRating(CallRatingEntity rating) {
        CallRatingEntity updatedRating = callRatingRepository.save(rating);
        CallRatingEntity ratingWithDetails = callRatingRepository.findByIdWithDetails(updatedRating.getId())
                .orElse(updatedRating);
        delete(ratingWithDetails.getId());
        insert(ratingWithDetails.getId(), ratingWithDetails);
        return ratingWithDetails;
    }

    public void removeRating(Long ratingId) {
        callRatingRepository.deleteById(ratingId);
        delete(ratingId);
    }

    public CallRatingEntity findById(Long id) {
        CallRatingEntity rating = search(id);
        if (rating == null) {
            rating = callRatingRepository.findByIdWithDetails(id).orElse(null);
            if (rating != null) {
                insert(rating.getId(), rating);
            }
        }
        return rating;
    }

    public List<CallRatingEntity> findByRatingGreaterThanEqual(Integer rating) {
        return callRatingRepository.findByRatingGreaterThanEqual(rating);
    }

    public List<CallRatingEntity> getAllRatingsSorted() {
        return inOrderTraversal();
    }

    public void reload() {
        loadFromDatabase();
    }
}
