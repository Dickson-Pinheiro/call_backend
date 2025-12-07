package com.group_call.call_backend.websocket;

import com.group_call.call_backend.security.JwtTokenProvider;
import com.group_call.call_backend.service.MatchmakingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    
    private final JwtTokenProvider tokenProvider;
    private final MatchmakingService matchmakingService;

    public WebSocketAuthInterceptor(JwtTokenProvider tokenProvider, @Lazy MatchmakingService matchmakingService) {
        this.tokenProvider = tokenProvider;
        this.matchmakingService = matchmakingService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                if (tokenProvider.validateToken(token)) {
                    Long userId = tokenProvider.getUserIdFromToken(token);
                    String email = tokenProvider.getEmailFromToken(token);
                    
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userId, null, null);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    accessor.setUser(() -> userId.toString());
                    accessor.setHeader("userId", userId);
                    accessor.setHeader("email", email);
                    
                } else {
                    throw new IllegalArgumentException("Token JWT inválido");
                }
            } else {
                throw new IllegalArgumentException("Token JWT não fornecido");
            }
        } else if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            Object userIdObj = accessor.getHeader("userId");
            
            if (userIdObj != null) {
                Long userId = null;
                
                if (userIdObj instanceof Long) {
                    userId = (Long) userIdObj;
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                    }
                }
                
                if (userId == null && accessor.getUser() != null) {
                    try {
                        userId = Long.parseLong(accessor.getUser().getName());
                    } catch (NumberFormatException e) {
                    }
                }
                
                if (userId != null) {
                    
                    try {
                        matchmakingService.cleanupUserOnDisconnect(userId);
                    } catch (Exception e) {
                    }
                } else {
                }
            }
        }

        return message;
    }
}
