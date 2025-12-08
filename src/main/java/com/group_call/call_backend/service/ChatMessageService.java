package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.ChatMessageEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.repository.ChatMessageRepository;
import com.group_call.call_backend.tree.ChatMessageTree;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageTree chatMessageTree;
    private final ChatMessageRepository chatMessageRepository;
    private final CallService callService;
    private final UserService userService;

    public ChatMessageService(ChatMessageTree chatMessageTree, ChatMessageRepository chatMessageRepository,
                            CallService callService, UserService userService) {
        this.chatMessageTree = chatMessageTree;
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

        return chatMessageTree.addMessage(message);
    }

    public ChatMessageEntity findById(Long id) {
        ChatMessageEntity message = chatMessageTree.findById(id);
        if (message == null) {
            message = chatMessageRepository.findById(id).orElse(null);
        }
        if (message == null) {
            throw new IllegalArgumentException("Mensagem não encontrada com ID: " + id);
        }
        return message;
    }

    public List<ChatMessageEntity> getAllMessages() {
        return chatMessageTree.getAllMessagesSorted();
    }

    public List<ChatMessageEntity> findByCallId(Long callId) {
        callService.findById(callId);
        return chatMessageTree.findByCallId(callId);
    }

    public ChatMessageEntity updateMessage(Long messageId, String newText) {
        ChatMessageEntity message = findById(messageId);
        message.setMessageText(newText);
        return chatMessageTree.updateMessage(message);
    }

    public void deleteMessage(Long messageId) {
        findById(messageId);
        chatMessageTree.removeMessage(messageId);
    }

    public void deleteMessagesByCallId(Long callId) {
        List<ChatMessageEntity> messages = findByCallId(callId);
        for (ChatMessageEntity message : messages) {
            chatMessageTree.removeMessage(message.getId());
        }
    }

    public long countMessagesByCallId(Long callId) {
        return findByCallId(callId).size();
    }

    public void reloadTree() {
        chatMessageTree.reload();
    }

    public void syncMessageAction(String action, Long messageId) {
        switch (action) {
            case "ADD":
                ChatMessageEntity message = chatMessageRepository.findById(messageId).orElse(null);
                if (message != null) {
                    chatMessageTree.insert(message.getId(), message);
                }
                break;
            case "REMOVE":
                chatMessageTree.removeMessage(messageId);
                break;
            default:
                throw new IllegalArgumentException("Ação desconhecida: " + action);
        }
    }
}
