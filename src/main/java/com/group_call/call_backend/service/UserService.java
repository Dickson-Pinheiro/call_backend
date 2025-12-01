package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.tree.UserTree;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service para gerenciar usuários utilizando a árvore AVL
 */
@Service
public class UserService {

    private final UserTree userTree;

    public UserService(UserTree userTree) {
        this.userTree = userTree;
    }

    /**
     * Cria um novo usuário
     */
    public UserEntity createUser(UserEntity user) {
        // Valida se o email já existe
        if (userTree.findByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException("Email já cadastrado: " + user.getEmail());
        }
        return userTree.addUser(user);
    }

    /**
     * Busca um usuário por ID (usa a árvore AVL - O(log n))
     */
    public UserEntity findById(Long id) {
        UserEntity user = userTree.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado com ID: " + id);
        }
        return user;
    }

    /**
     * Busca um usuário por email
     */
    public UserEntity findByEmail(String email) {
        UserEntity user = userTree.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado com email: " + email);
        }
        return user;
    }

    /**
     * Lista todos os usuários ordenados por ID
     */
    public List<UserEntity> getAllUsers() {
        return userTree.getAllUsersSorted();
    }

    /**
     * Atualiza um usuário
     */
    public UserEntity updateUser(Long id, UserEntity updatedUser) {
        UserEntity existingUser = findById(id);
        
        // Verifica se o email foi alterado e se já existe
        if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
            UserEntity userWithEmail = userTree.findByEmail(updatedUser.getEmail());
            if (userWithEmail != null && !userWithEmail.getId().equals(id)) {
                throw new IllegalArgumentException("Email já cadastrado: " + updatedUser.getEmail());
            }
        }

        updatedUser.setId(id);
        return userTree.updateUser(updatedUser);
    }

    /**
     * Atualiza status online do usuário
     */
    public UserEntity updateOnlineStatus(Long id, boolean isOnline) {
        UserEntity user = findById(id);
        user.setIsOnline(isOnline);
        return userTree.updateUser(user);
    }

    /**
     * Ativa/desativa um usuário
     */
    public UserEntity updateActiveStatus(Long id, boolean isActive) {
        UserEntity user = findById(id);
        user.setIsActive(isActive);
        return userTree.updateUser(user);
    }

    /**
     * Remove um usuário
     */
    public void deleteUser(Long id) {
        findById(id); // Valida se existe
        userTree.removeUser(id);
    }

    /**
     * Verifica se um email já está cadastrado
     */
    public boolean emailExists(String email) {
        return userTree.findByEmail(email) != null;
    }

    /**
     * Recarrega os dados do banco para a árvore
     */
    public void reloadTree() {
        userTree.reload();
    }
}
