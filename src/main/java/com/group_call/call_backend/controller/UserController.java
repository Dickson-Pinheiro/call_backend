package com.group_call.call_backend.controller;

import com.group_call.call_backend.dto.UserCreateRequest;
import com.group_call.call_backend.dto.UserResponse;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserEntity user = new UserEntity();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setIsActive(true);
        user.setIsOnline(false);

        UserEntity createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(createdUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserEntity user = userService.findById(id);
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserEntity user = userService.findByEmail(email);
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserEntity> users = userService.getAllUsers();
        List<UserResponse> response = users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserCreateRequest request) {
        
        UserEntity user = new UserEntity();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        UserEntity updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(toResponse(updatedUser));
    }

    @PatchMapping("/{id}/online")
    public ResponseEntity<UserResponse> updateOnlineStatus(
            @PathVariable Long id,
            @RequestParam boolean isOnline) {
        
        UserEntity user = userService.updateOnlineStatus(id, isOnline);
        return ResponseEntity.ok(toResponse(user));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<UserResponse> updateActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean isActive) {
        
        UserEntity user = userService.updateActiveStatus(id, isActive);
        return ResponseEntity.ok(toResponse(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(UserEntity user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setIsActive(user.getIsActive());
        response.setIsOnline(user.getIsOnline());
        return response;
    }
}
