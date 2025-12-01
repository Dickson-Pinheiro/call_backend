package com.group_call.call_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallRatingCreateRequest {

    @NotNull(message = "ID da chamada é obrigatório")
    private Long callId;

    @NotNull(message = "ID do avaliador é obrigatório")
    private Long raterId;

    @NotNull(message = "Avaliação é obrigatória")
    @Min(value = 1, message = "Avaliação mínima é 1")
    @Max(value = 5, message = "Avaliação máxima é 5")
    private Integer rating;

    private String comment;
}
