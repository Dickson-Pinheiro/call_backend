package com.group_call.call_backend.controller;

import com.group_call.call_backend.dto.AuthResponse;
import com.group_call.call_backend.dto.LoginRequest;
import com.group_call.call_backend.dto.UserCreateRequest;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody UserCreateRequest request) {
        UserEntity user = authService.signup(
                request.getName(),
                request.getEmail(),
                request.getPassword()
        );

        String token = authService.login(request.getEmail(), request.getPassword());

        AuthResponse response = new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        UserEntity user = authService.getUserByEmail(request.getEmail());

        AuthResponse response = new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() != null) {
            Long userId = (Long) authentication.getPrincipal();
            authService.logout(userId);
        }
        return ResponseEntity.noContent().build();
    }
}
