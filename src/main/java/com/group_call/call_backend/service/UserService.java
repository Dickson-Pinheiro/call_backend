package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.UserRepository;
import com.group_call.call_backend.tree.UserTree;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserTree userTree;
    private final UserRepository userRepository;

    public UserService(UserTree userTree, UserRepository userRepository) {
        this.userTree = userTree;
        this.userRepository = userRepository;
    }

    public UserEntity createUser(UserEntity user) {
        if (userTree.findByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException("Email já cadastrado: " + user.getEmail());
        }
        return userTree.addUser(user);
    }

    public UserEntity findById(Long id) {
        UserEntity user = userTree.findById(id);
        if (user == null) {
            user = userRepository.findById(id).orElse(null);
        }
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado com ID: " + id);
        }
        return user;
    }

    public UserEntity findByEmail(String email) {
        UserEntity user = userTree.findByEmail(email);
        if (user == null) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado com email: " + email);
        }
        return user;
    }

    public List<UserEntity> getAllUsers() {
        return userTree.getAllUsersSorted();
    }

    public UserEntity updateUser(Long id, UserEntity updatedUser) {
        UserEntity existingUser = findById(id);
        
        if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
            UserEntity userWithEmail = userTree.findByEmail(updatedUser.getEmail());
            if (userWithEmail != null && !userWithEmail.getId().equals(id)) {
                throw new IllegalArgumentException("Email já cadastrado: " + updatedUser.getEmail());
            }
        }

        updatedUser.setId(id);
        return userTree.updateUser(updatedUser);
    }

    public UserEntity updateOnlineStatus(Long id, boolean isOnline) {
        UserEntity user = findById(id);
        user.setIsOnline(isOnline);
        return userTree.updateUser(user);
    }

    public UserEntity updateActiveStatus(Long id, boolean isActive) {
        UserEntity user = findById(id);
        user.setIsActive(isActive);
        return userTree.updateUser(user);
    }

    public void deleteUser(Long id) {
        findById(id);
        userTree.removeUser(id);
    }

    public boolean emailExists(String email) {
        return userTree.findByEmail(email) != null;
    }

    public void reloadTree() {
        userTree.reload();
    }

    public void syncUserAction(String action, Long userId) {
        switch (action) {
            case "ADD":
                UserEntity user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    userTree.insert(user.getId(), user);
                }
                break;
            case "REMOVE":
                userTree.removeUser(userId);
                break;
            default:
                throw new IllegalArgumentException("Ação desconhecida: " + action);
        }
    }
}
