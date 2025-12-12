package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.ChatMessageEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final CallService callService;
    private final UserService userService;

    public ChatMessageService(ChatMessageRepository chatMessageRepository,
            CallService callService, UserService userService) {
        this.chatMessageRepository = chatMessageRepository;
        this.callService = callService;
        this.userService = userService;
    }

    public ChatMessageEntity createMessage(Long callId, Long senderId, String messageText) {
        CallEntity call = callService.findById(callId);
        UserEntity sender = userService.findById(senderId);

        if (!call.getUser1().getId().equals(senderId) && !call.getUser2().getId().equals(senderId)) {
            throw new IllegalArgumentException("Usuário não faz parte desta chamada");
        }

        if (!call.getStatus().equals(CallEntity.CallStatus.ACTIVE)) {
            throw new IllegalStateException("Não é possível enviar mensagens em chamadas finalizadas");
        }

        ChatMessageEntity message = new ChatMessageEntity();
        message.setCall(call);
        message.setSender(sender);
        message.setMessageText(messageText);

        return chatMessageRepository.save(message);
    }

    public ChatMessageEntity findById(Long id) {
        return chatMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada com ID: " + id));
    }

    public List<ChatMessageEntity> getAllMessages() {
        return chatMessageRepository.findAll();
    }

    public List<ChatMessageEntity> findByCallId(Long callId) {
        callService.findById(callId);
        return chatMessageRepository.findByCallIdOrderBySentAtAsc(callId);
    }

    public ChatMessageEntity updateMessage(Long messageId, String newText) {
        ChatMessageEntity message = findById(messageId);
        message.setMessageText(newText);
        return chatMessageRepository.save(message);
    }

    public void deleteMessage(Long messageId) {
        if (!chatMessageRepository.existsById(messageId)) {
            throw new IllegalArgumentException("Mensagem não encontrada com ID: " + messageId);
        }
        chatMessageRepository.deleteById(messageId);
    }

    public void deleteMessagesByCallId(Long callId) {
        List<ChatMessageEntity> messages = findByCallId(callId);
        chatMessageRepository.deleteAll(messages);
    }

    public long countMessagesByCallId(Long callId) {
        return findByCallId(callId).size();
    }
}
