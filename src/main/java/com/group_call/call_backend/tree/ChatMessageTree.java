package com.group_call.call_backend.tree;

import com.group_call.call_backend.entity.ChatMessageEntity;
import com.group_call.call_backend.repository.ChatMessageRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatMessageTree extends AVLTree<ChatMessageEntity> {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageTree(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
        loadFromDatabase();
    }

    public void loadFromDatabase() {
        clear();
        List<ChatMessageEntity> messages = chatMessageRepository.findAll();
        for (ChatMessageEntity message : messages) {
            insert(message.getId(), message);
        }
    }

    public ChatMessageEntity addMessage(ChatMessageEntity message) {
        ChatMessageEntity savedMessage = chatMessageRepository.save(message);
        insert(savedMessage.getId(), savedMessage);
        return savedMessage;
    }

    public ChatMessageEntity updateMessage(ChatMessageEntity message) {
        ChatMessageEntity updatedMessage = chatMessageRepository.save(message);
        delete(updatedMessage.getId());
        insert(updatedMessage.getId(), updatedMessage);
        return updatedMessage;
    }

    public void removeMessage(Long messageId) {
        chatMessageRepository.deleteById(messageId);
        delete(messageId);
    }

    public ChatMessageEntity findById(Long id) {
        return search(id);
    }

    public List<ChatMessageEntity> findByCallId(Long callId) {
        return chatMessageRepository.findByCallIdOrderBySentAtAsc(callId);
    }

    public List<ChatMessageEntity> getAllMessagesSorted() {
        return inOrderTraversal();
    }

    public void reload() {
        loadFromDatabase();
    }
}
