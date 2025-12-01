package com.group_call.call_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallCreateRequest {

    @NotNull(message = "ID do usuário 1 é obrigatório")
    private Long user1Id;

    @NotNull(message = "ID do usuário 2 é obrigatório")
    private Long user2Id;

    private String callType;
}
