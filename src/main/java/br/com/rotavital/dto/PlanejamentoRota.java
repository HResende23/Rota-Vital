package br.com.rotavital.dto;

import java.time.LocalDateTime;
import java.util.List;
import br.com.rotavital.model.PontoDaMalha;

/** Resultado da simulação: caminhos comparados e a alternativa proposta (a mais curta viável). */
public record PlanejamentoRota(
    RemessaResposta remessa,
    PontoDaMalha origem,
    PontoDaMalha destino,
    ParametrosRota parametros,
    LocalDateTime janelaInicio,
    LocalDateTime janelaFim,
    boolean horarioDePico,
    List<AlternativaRota> alternativas,
    AlternativaRota proposta
) {
    public PlanejamentoRota {
        alternativas = List.copyOf(alternativas);
    }

    public boolean isViavel() {
        return proposta != null;
    }

    /** Alternativa cujos critérios aparecem no painel: a proposta ou, sem proposta, a mais rápida. */
    public AlternativaRota getEmDestaque() {
        return proposta != null ? proposta : alternativas.isEmpty() ? null : alternativas.get(0);
    }

    public boolean propostaUsa(String chaveDoTrecho) {
        return proposta != null && proposta.usaTrecho(chaveDoTrecho);
    }

    public boolean propostaPassaPor(String codigoDoPonto) {
        return proposta != null && proposta.passaPor(codigoDoPonto);
    }

    public boolean alternativaUsa(String chaveDoTrecho) {
        return alternativas.stream().anyMatch(a -> a.usaTrecho(chaveDoTrecho));
    }
}
