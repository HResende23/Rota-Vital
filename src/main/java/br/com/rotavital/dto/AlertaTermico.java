package br.com.rotavital.dto;

import java.time.LocalDateTime;

/** Alerta confirmado por duas leituras consecutivas fora da faixa. */
public record AlertaTermico(LocalDateTime desvioIniciadoEm, LocalDateTime emitidoEm,
                            LocalDateTime reconhecidoEm, String reconhecidoPor) {
    public boolean isReconhecido() {
        return reconhecidoEm != null;
    }
}
