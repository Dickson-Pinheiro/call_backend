package com.group_call.call_backend.websocket;

import com.group_call.call_backend.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public WebSocketAuthInterceptor(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            logger.info(">>> [WS_AUTH] Tentativa de conexão WebSocket - header presente: {}", authHeader != null);
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                if (tokenProvider.validateToken(token)) {
                    Long userId = tokenProvider.getUserIdFromToken(token);
                    String email = tokenProvider.getEmailFromToken(token);
                    
                    logger.info(">>> [WS_AUTH] Token validado com sucesso - userId={}, email={}", userId, email);
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userId, null, null);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    accessor.setUser(() -> userId.toString());
                    accessor.setHeader("userId", userId);
                    accessor.setHeader("email", email);
                    
                    logger.info(">>> [WS_AUTH] Usuário autenticado - userId={}", userId);
                } else {
                    logger.error(">>> [WS_AUTH] Token inválido");
                    throw new IllegalArgumentException("Token JWT inválido");
                }
            } else {
                logger.error(">>> [WS_AUTH] Token não fornecido ou formato incorreto");
                throw new IllegalArgumentException("Token JWT não fornecido");
            }
        }

        return message;
    }
}
