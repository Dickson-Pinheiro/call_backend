package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity createUser(UserEntity user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado: " + user.getEmail());
        }
        return userRepository.save(user);
    }

    public UserEntity findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com ID: " + id));
    }

    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com email: " + email));
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public UserEntity updateUser(Long id, UserEntity updatedUser) {
        UserEntity existingUser = findById(id);

        if (updatedUser.getEmail() != null && !existingUser.getEmail().equals(updatedUser.getEmail())) {
            if (userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email já cadastrado: " + updatedUser.getEmail());
            }
        }

        // Maintain other fields if needed, but assuming updatedUser has all fields or
        // we merge
        // For now, following logic of previous implementation which set ID and called
        // update
        updatedUser.setId(id);
        return userRepository.save(updatedUser);
    }

    public UserEntity updateOnlineStatus(Long id, boolean isOnline) {
        UserEntity user = findById(id);
        user.setIsOnline(isOnline);
        return userRepository.save(user);
    }

    public UserEntity updateActiveStatus(Long id, boolean isActive) {
        UserEntity user = findById(id);
        user.setIsActive(isActive);
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuário não encontrado com ID: " + id);
        }
        userRepository.deleteById(id);
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
