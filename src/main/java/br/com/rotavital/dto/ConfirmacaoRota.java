package br.com.rotavital.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import jakarta.validation.constraints.*;

/** Corpo da API para confirmar o planejamento. Sem caminho, confirma a alternativa proposta. */
public record ConfirmacaoRota(
    @NotBlank @Size(max = 40) String destino,
    @NotNull LocalDateTime saida,
    @NotNull LocalTime janelaInicio,
    @NotNull LocalTime janelaFim,
    @Size(max = 200) String caminho
) {
    public ParametrosRota parametros() {
        return new ParametrosRota(destino, saida, janelaInicio, janelaFim);
    }
}
