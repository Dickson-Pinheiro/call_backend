package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.UserRepository;
import com.group_call.call_backend.security.JwtTokenProvider;
import com.group_call.call_backend.tree.UserTree;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserTree userTree;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MatchmakingService matchmakingService;

    public AuthService(UserTree userTree, UserRepository userRepository, 
                      JwtTokenProvider tokenProvider,
                      @Lazy MatchmakingService matchmakingService) {
        this.userTree = userTree;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.matchmakingService = matchmakingService;
    }

    public UserEntity signup(String name, String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setIsOnline(false);
        return userTree.addUser(user);
    }

    public String login(String email, String password) {
        UserEntity user = userTree.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Email ou senha inválidos");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Email ou senha inválidos");
        }

        if (!user.getIsActive()) {
            throw new IllegalStateException("Usuário inativo");
        }

        user.setIsOnline(true);
        
        userTree.updateUser(user);

        return tokenProvider.generateToken(user.getId(), user.getEmail());
    }

    public UserEntity getUserByEmail(String email) {
        UserEntity user = userTree.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        return user;
    }

    public void logout(Long userId) {
        
        UserEntity user = userTree.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        
        try {
            matchmakingService.cleanupUserOnDisconnect(userId);
        } catch (Exception e) {
        }
        
        user.setIsOnline(false);
        userTree.updateUser(user);
        
    }
}
