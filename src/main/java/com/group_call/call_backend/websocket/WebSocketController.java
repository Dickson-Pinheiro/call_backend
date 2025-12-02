package com.group_call.call_backend.websocket;

import com.group_call.call_backend.dto.ChatMessage;
import com.group_call.call_backend.dto.WebRTCSignal;
import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.ChatMessageEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.service.ChatMessageService;
import com.group_call.call_backend.service.MatchmakingService;
import com.group_call.call_backend.tree.CallTree;
import com.group_call.call_backend.tree.UserTree;
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
    private final CallTree callTree;
    private final UserTree userTree;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(
            MatchmakingService matchmakingService,
            ChatMessageService chatMessageService,
            CallTree callTree,
            UserTree userTree,
            SimpMessagingTemplate messagingTemplate) {
        this.matchmakingService = matchmakingService;
        this.chatMessageService = chatMessageService;
        this.callTree = callTree;
        this.userTree = userTree;
        this.messagingTemplate = messagingTemplate;
    }

    @SubscribeMapping("/connect")
    public void handleConnect(SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        String sessionId = headerAccessor.getSessionId();

        matchmakingService.registerSession(userId, sessionId);

        logger.info(">>> [WS_CONNECT] Usuário conectado - userId={}, sessionId={}", userId, sessionId);
    }

    @MessageMapping("/join-queue")
    public void joinQueue(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        
        logger.info(">>> [WS_JOIN_QUEUE] Recebida requisição - userId={}, principal.name={}", 
                    userId, principal.getName());

        try {
            matchmakingService.joinQueue(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "waiting");
            response.put("message", "Procurando alguém para conversar...");

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/status",
                    response);

            logger.info(">>> [WS_JOIN_QUEUE] Resposta enviada - userId={}", userId);
        } catch (IllegalStateException e) {
            logger.error(">>> [WS_JOIN_QUEUE] Erro - userId={}, error={}", userId, e.getMessage());
            sendError(userId, e.getMessage());
        }
    }

    @MessageMapping("/leave-queue")
    public void leaveQueue(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        matchmakingService.leaveQueue(userId);

        logger.info("Usuário {} saiu da fila", userId);
    }

    @MessageMapping("/next-person")
    public void nextPerson(Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        logger.info("Usuário {} solicitou próxima pessoa", userId);
        matchmakingService.nextPerson(userId);
    }

    @MessageMapping("/end-call")
    public void endCall(@Payload Map<String, Object> payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        Long callId = Long.parseLong(payload.get("callId").toString());

        logger.info("Usuário {} encerrando chamada {}", userId, callId);
        matchmakingService.endCall(callId, userId);
    }

    @MessageMapping("/webrtc-signal")
    public void handleWebRTCSignal(@Payload WebRTCSignal signal, Principal principal) {
        Long senderId = Long.parseLong(principal.getName());
        Long targetUserId = signal.getTargetUserId();

        messagingTemplate.convertAndSendToUser(
                targetUserId.toString(),
                "/queue/webrtc-signal",
                Map.of(
                        "type", signal.getType(),
                        "senderId", senderId,
                        "data", signal.getData()));

        logger.debug("Sinal WebRTC {} enviado de {} para {}",
                signal.getType(), senderId, targetUserId);
    }

    @MessageMapping("/chat-message")
    public void handleChatMessage(@Payload ChatMessage message, Principal principal) {
        Long senderId = Long.parseLong(principal.getName());
        Long callId = message.getCallId();

        CallEntity call = callTree.findById(callId);
        if (call == null) {
            sendError(senderId, "Chamada não encontrada");
            return;
        }

        UserEntity sender = userTree.findById(senderId);
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
        response.put("message", message.getMessage());
        response.put("sentAt", chatMessage.getSentAt().toString());

        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/chat",
                response);

        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/chat",
                response);

        logger.info("Mensagem enviada na chamada {} de {} para {}",
                callId, senderId, recipientId);
    }

    @MessageMapping("/typing")
    public void handleTyping(@Payload Map<String, Object> payload, Principal principal) {
        Long senderId = Long.parseLong(principal.getName());
        Long callId = Long.parseLong(payload.get("callId").toString());

        CallEntity call = callTree.findById(callId);
        if (call == null)
            return;

        Long recipientId = call.getUser1().getId().equals(senderId)
                ? call.getUser2().getId()
                : call.getUser1().getId();

        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/typing",
                Map.of("isTyping", true));
    }

    private void sendError(Long userId, String message) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/error",
                Map.of("error", message));
    }
}
