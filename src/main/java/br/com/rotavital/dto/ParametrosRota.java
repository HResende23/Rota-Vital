package br.com.rotavital.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.*;

/**
 * Dados informados pelo planejador: hospital de destino na malha, saída prevista do
 * hemocentro e janela de recebimento informada pelo hospital.
 */
public record ParametrosRota(
    @NotBlank @Size(max = 40) String destino,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime saida,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime janelaInicio,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime janelaFim
) {
    /** Início da janela na data da saída; se a janela já tiver terminado, considera o dia seguinte. */
    public LocalDateTime inicioDaJanela() {
        LocalDateTime inicio = saida.toLocalDate().atTime(janelaInicio);
        return saida.toLocalDate().atTime(janelaFim).isBefore(saida) ? inicio.plusDays(1) : inicio;
    }

    public LocalDateTime fimDaJanela() {
        LocalDateTime fim = saida.toLocalDate().atTime(janelaFim);
        return fim.isBefore(saida) ? fim.plusDays(1) : fim;
    }
}
