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
        List<CallRatingEntity> ratings = callRatingRepository.findAll();
        for (CallRatingEntity rating : ratings) {
            insert(rating.getId(), rating);
        }
    }

    public CallRatingEntity addRating(CallRatingEntity rating) {
        CallRatingEntity savedRating = callRatingRepository.save(rating);
        insert(savedRating.getId(), savedRating);
        return savedRating;
    }

    public CallRatingEntity updateRating(CallRatingEntity rating) {
        CallRatingEntity updatedRating = callRatingRepository.save(rating);
        // Remove e reinsere para garantir atualização
        delete(updatedRating.getId());
        insert(updatedRating.getId(), updatedRating);
        return updatedRating;
    }

    public void removeRating(Long ratingId) {
        callRatingRepository.deleteById(ratingId);
        delete(ratingId);
    }

    public CallRatingEntity findById(Long id) {
        return search(id);
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
