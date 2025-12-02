package com.group_call.call_backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component
public class AuthenticationHelper {

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        throw new IllegalStateException("Usuário não autenticado");
    }

    public boolean isOwner(Long resourceOwnerId) {
        return getCurrentUserId().equals(resourceOwnerId);
    }

    public boolean isInCall(Long user1Id, Long user2Id) {
        Long currentUserId = getCurrentUserId();
        return currentUserId.equals(user1Id) || currentUserId.equals(user2Id);
    }
}
