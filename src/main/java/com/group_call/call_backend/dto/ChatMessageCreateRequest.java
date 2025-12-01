package com.group_call.call_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageCreateRequest {

    @NotNull(message = "ID da chamada é obrigatório")
    private Long callId;

    @NotNull(message = "ID do remetente é obrigatório")
    private Long senderId;

    @NotBlank(message = "Mensagem não pode estar vazia")
    private String messageText;
}
