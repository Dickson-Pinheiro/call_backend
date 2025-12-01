package com.group_call.call_backend.tree;

import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserTree extends AVLTree<UserEntity> {

    private final UserRepository userRepository;

    public UserTree(UserRepository userRepository) {
        this.userRepository = userRepository;
        loadFromDatabase();
    }

    public void loadFromDatabase() {
        clear();
        List<UserEntity> users = userRepository.findAll();
        for (UserEntity user : users) {
            insert(user.getId(), user);
        }
    }

    public UserEntity addUser(UserEntity user) {
        UserEntity savedUser = userRepository.save(user);
        insert(savedUser.getId(), savedUser);
        return savedUser;
    }

    public UserEntity updateUser(UserEntity user) {
        UserEntity updatedUser = userRepository.save(user);
        delete(updatedUser.getId());
        insert(updatedUser.getId(), updatedUser);
        return updatedUser;
    }

    public void removeUser(Long userId) {
        userRepository.deleteById(userId);
        delete(userId);
    }

    public UserEntity findById(Long id) {
        return search(id);
    }

    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public List<UserEntity> getAllUsersSorted() {
        return inOrderTraversal();
    }

    public void reload() {
        loadFromDatabase();
    }
}
