package com.group_call.call_backend.service;

import com.group_call.call_backend.entity.CallEntity;
import com.group_call.call_backend.entity.ChatMessageEntity;
import com.group_call.call_backend.entity.UserEntity;
import com.group_call.call_backend.tree.ChatMessageTree;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service para gerenciar mensagens de chat utilizando a árvore AVL
 */
@Service
public class ChatMessageService {

    private final ChatMessageTree chatMessageTree;
    private final CallService callService;
    private final UserService userService;

    public ChatMessageService(ChatMessageTree chatMessageTree, CallService callService, UserService userService) {
        this.chatMessageTree = chatMessageTree;
        this.callService = callService;
        this.userService = userService;
    }

    /**
     * Cria uma nova mensagem
     */
    public ChatMessageEntity createMessage(Long callId, Long senderId, String messageText) {
        CallEntity call = callService.findById(callId);
        UserEntity sender = userService.findById(senderId);

        // Valida se o remetente faz parte da chamada
        if (!call.getUser1().getId().equals(senderId) && !call.getUser2().getId().equals(senderId)) {
            throw new IllegalArgumentException("Usuário não faz parte desta chamada");
        }

        // Valida se a chamada está ativa
        if (!call.getStatus().equals(CallEntity.CallStatus.ACTIVE)) {
            throw new IllegalStateException("Não é possível enviar mensagens em chamadas finalizadas");
        }

        ChatMessageEntity message = new ChatMessageEntity();
        message.setCall(call);
        message.setSender(sender);
        message.setMessageText(messageText);

        return chatMessageTree.addMessage(message);
    }

    /**
     * Busca uma mensagem por ID (usa a árvore AVL - O(log n))
     */
    public ChatMessageEntity findById(Long id) {
        ChatMessageEntity message = chatMessageTree.findById(id);
        if (message == null) {
            throw new IllegalArgumentException("Mensagem não encontrada com ID: " + id);
        }
        return message;
    }

    /**
     * Lista todas as mensagens ordenadas por ID
     */
    public List<ChatMessageEntity> getAllMessages() {
        return chatMessageTree.getAllMessagesSorted();
    }

    /**
     * Busca mensagens por ID da chamada (ordenadas por data de envio)
     */
    public List<ChatMessageEntity> findByCallId(Long callId) {
        callService.findById(callId); // Valida se a chamada existe
        return chatMessageTree.findByCallId(callId);
    }

    /**
     * Atualiza o texto de uma mensagem
     */
    public ChatMessageEntity updateMessage(Long messageId, String newText) {
        ChatMessageEntity message = findById(messageId);
        message.setMessageText(newText);
        return chatMessageTree.updateMessage(message);
    }

    /**
     * Remove uma mensagem
     */
    public void deleteMessage(Long messageId) {
        findById(messageId); // Valida se existe
        chatMessageTree.removeMessage(messageId);
    }

    /**
     * Remove todas as mensagens de uma chamada
     */
    public void deleteMessagesByCallId(Long callId) {
        List<ChatMessageEntity> messages = findByCallId(callId);
        for (ChatMessageEntity message : messages) {
            chatMessageTree.removeMessage(message.getId());
        }
    }

    /**
     * Conta o número de mensagens em uma chamada
     */
    public long countMessagesByCallId(Long callId) {
        return findByCallId(callId).size();
    }

    /**
     * Recarrega os dados do banco para a árvore
     */
    public void reloadTree() {
        chatMessageTree.reload();
    }
}
