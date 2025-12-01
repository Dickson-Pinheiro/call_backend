package com.group_call.call_backend.controller;

import com.group_call.call_backend.dto.ChatMessageCreateRequest;
import com.group_call.call_backend.dto.ChatMessageResponse;
import com.group_call.call_backend.entity.ChatMessageEntity;
import com.group_call.call_backend.service.ChatMessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @PostMapping
    public ResponseEntity<ChatMessageResponse> createMessage(@Valid @RequestBody ChatMessageCreateRequest request) {
        ChatMessageEntity message = chatMessageService.createMessage(
                request.getCallId(),
                request.getSenderId(),
                request.getMessageText()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(message));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatMessageResponse> getMessageById(@PathVariable Long id) {
        ChatMessageEntity message = chatMessageService.findById(id);
        return ResponseEntity.ok(toResponse(message));
    }

    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> getAllMessages() {
        List<ChatMessageEntity> messages = chatMessageService.getAllMessages();
        List<ChatMessageResponse> response = messages.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/call/{callId}")
    public ResponseEntity<List<ChatMessageResponse>> getMessagesByCallId(@PathVariable Long callId) {
        List<ChatMessageEntity> messages = chatMessageService.findByCallId(callId);
        List<ChatMessageResponse> response = messages.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/call/{callId}/count")
    public ResponseEntity<Long> countMessagesByCallId(@PathVariable Long callId) {
        long count = chatMessageService.countMessagesByCallId(callId);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChatMessageResponse> updateMessage(
            @PathVariable Long id,
            @RequestParam String messageText) {
        
        if (messageText == null || messageText.trim().isEmpty()) {
            throw new IllegalArgumentException("Mensagem não pode estar vazia");
        }

        ChatMessageEntity message = chatMessageService.updateMessage(id, messageText);
        return ResponseEntity.ok(toResponse(message));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        chatMessageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/call/{callId}")
    public ResponseEntity<Void> deleteMessagesByCallId(@PathVariable Long callId) {
        chatMessageService.deleteMessagesByCallId(callId);
        return ResponseEntity.noContent().build();
    }

    private ChatMessageResponse toResponse(ChatMessageEntity message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setCallId(message.getCall().getId());
        response.setSenderId(message.getSender().getId());
        response.setSenderName(message.getSender().getName());
        response.setMessageText(message.getMessageText());
        response.setSentAt(message.getSentAt());
        return response;
    }
}
