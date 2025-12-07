package com.group_call.call_backend.websocket;

import com.group_call.call_backend.dto.ChatMessage;
import com.group_call.call_backend.dto.WebRTCSignal;
import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.ChatMessageEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.CallRepository;
import com.group_call.call_backend.repository.UserRepository;
import com.group_call.call_backend.service.ChatMessageService;
import com.group_call.call_backend.service.MatchmakingService;
import com.group_call.call_backend.service.RedisWebSocketBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    private final MatchmakingService matchmakingService;
    private final ChatMessageService chatMessageService;
    private final UserRepository userRepository;
    private final CallRepository callRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisWebSocketBroadcastService redisBroadcast;

    public WebSocketController(
            MatchmakingService matchmakingService,
            ChatMessageService chatMessageService,
            UserRepository userRepository,
            CallRepository callRepository,
            SimpMessagingTemplate messagingTemplate,
            RedisWebSocketBroadcastService redisBroadcast) {
        this.matchmakingService = matchmakingService;
        this.chatMessageService = chatMessageService;
        this.userRepository = userRepository;
        this.callRepository = callRepository;
        this.messagingTemplate = messagingTemplate;
        this.redisBroadcast = redisBroadcast;
    }

    @SubscribeMapping("/connect")
    public void handleConnect(SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        String sessionId = headerAccessor.getSessionId();
        matchmakingService.registerSession(userId, sessionId);
    }

    @MessageMapping("/join-queue")
    public void joinQueue(Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        try {
            matchmakingService.joinQueue(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "waiting");
            response.put("message", "Procurando alguém para conversar...");

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/status",
                    response);
        } catch (IllegalStateException e) {
            logger.error("Erro ao entrar na fila - userId={}: {}", userId, e.getMessage());
            sendError(userId, e.getMessage());
        }
    }

    @MessageMapping("/leave-queue")
    public void leaveQueue(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        matchmakingService.leaveQueue(userId);
    }

    @MessageMapping("/next-person")
    public void nextPerson(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        matchmakingService.nextPerson(userId);
    }

    @MessageMapping("/end-call")
    public void endCall(@Payload Map<String, Object> payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        Long callId = Long.parseLong(payload.get("callId").toString());
        matchmakingService.endCall(callId, userId);
    }

    @MessageMapping("/webrtc-signal")
    public void handleWebRTCSignal(@Payload WebRTCSignal signal, Principal principal) {
        try {
            Long senderId = Long.parseLong(principal.getName());
            Long targetUserId = signal.getTargetUserId();
            Long callId = signal.getCallId();

            if (targetUserId == null) {
                throw new IllegalArgumentException("targetUserId é obrigatório");
            }
            if (callId == null) {
                throw new IllegalArgumentException("callId é obrigatório");
            }

            Map<String, Object> signalData = new HashMap<>();
            signalData.put("type", signal.getType());
            signalData.put("senderId", senderId);
            signalData.put("targetUserId", targetUserId);
            signalData.put("callId", callId);
            signalData.put("data", signal.getData());

            redisBroadcast.broadcastWebRTCSignal(targetUserId, signalData);
        } catch (Exception e) {
            logger.error("Erro ao processar sinal WebRTC: {}", e.getMessage());
            throw e;
        }
    }

    @MessageMapping("/chat-message")
    public void handleChatMessage(@Payload ChatMessage message, Principal principal) {
        try {
            Long senderId = Long.parseLong(principal.getName());
            Long callId = message.getCallId();

            CallEntity call = callRepository.findById(callId).orElse(null);
            if (call == null || call.getStatus() != CallEntity.CallStatus.ACTIVE) {
                sendError(senderId, "Chamada encerrada");
                return;
            }

            UserEntity sender = userRepository.findById(senderId).orElse(null);
            if (sender == null) {
                sendError(senderId, "Usuário não encontrado");
                return;
            }

            ChatMessageEntity chatMessage = chatMessageService.createMessage(
                    callId,
                    senderId,
                    message.getMessage());

            Long recipientId = call.getUser1().getId().equals(senderId)
                    ? call.getUser2().getId()
                    : call.getUser1().getId();

            Map<String, Object> response = new HashMap<>();
            response.put("id", chatMessage.getId());
            response.put("callId", callId);
            response.put("senderId", senderId);
            response.put("senderName", sender.getName());
            response.put("recipientId", recipientId);
            response.put("message", message.getMessage());
            response.put("sentAt", chatMessage.getSentAt().toString());

            redisBroadcast.broadcastChatMessage(recipientId, response);
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação no chat: {}", e.getMessage());
            sendError(Long.parseLong(principal.getName()), e.getMessage());
        } catch (Exception e) {
            logger.error("Erro ao enviar mensagem: {}", e.getMessage());
            sendError(Long.parseLong(principal.getName()), "Erro ao enviar mensagem");
        }
    }

    @MessageMapping("/typing")
    public void handleTyping(@Payload Map<String, Object> payload, Principal principal) {
        try {
            Long senderId = Long.parseLong(principal.getName());
            Long callId = Long.parseLong(payload.get("callId").toString());
            boolean isTyping = payload.containsKey("isTyping") ? (Boolean) payload.get("isTyping") : true;

            CallEntity call = callRepository.findById(callId).orElse(null);
            if (call == null || call.getStatus() != CallEntity.CallStatus.ACTIVE) {
                return;
            }

            Long recipientId = call.getUser1().getId().equals(senderId)
                    ? call.getUser2().getId()
                    : call.getUser1().getId();

            Map<String, Object> typingData = Map.of(
                "isTyping", isTyping,
                "userId", senderId
            );
            
            redisBroadcast.broadcastTypingIndicator(recipientId, typingData);
        } catch (Exception e) {
            logger.error("Erro ao processar typing indicator: {}", e.getMessage());
        }
    }
    
    @MessageMapping("/logout")
    public void handleLogout(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        matchmakingService.cleanupUserOnDisconnect(userId);
    }

    private void sendError(Long userId, String message) {
        Map<String, Object> errorData = Map.of("error", message);
        redisBroadcast.broadcastError(userId, errorData);
    }
}
