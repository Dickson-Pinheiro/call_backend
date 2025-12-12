package com.group_call.call_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "call_ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "call_id", "rater_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallRatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Chamada é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false)
    private CallEntity call;

    @NotNull(message = "Avaliador é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rater_id", nullable = false)
    private UserEntity rater;

    @NotNull(message = "Avaliação é obrigatória")
    @Min(value = 1, message = "Avaliação mínima é 1")
    @Max(value = 5, message = "Avaliação máxima é 5")
    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
