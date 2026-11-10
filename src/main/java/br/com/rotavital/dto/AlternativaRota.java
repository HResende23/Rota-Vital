package br.com.rotavital.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Caminho candidato avaliado para uma remessa, com horários previstos e critérios de viabilidade. */
public record AlternativaRota(
    String chave,
    String descricao,
    List<String> pontos,
    List<String> trechos,
    int duracaoMinutos,
    LocalDateTime saida,
    LocalDateTime chegada,
    LocalDateTime inicioAtendimento,
    LocalDateTime fimAtendimento,
    int esperaMinutos,
    int tempoTotalMinutos,
    List<CriterioRota> criterios,
    boolean viavel,
    boolean proposta
) {
    public AlternativaRota {
        pontos = List.copyOf(pontos);
        trechos = List.copyOf(trechos);
        criterios = List.copyOf(criterios);
    }

    public AlternativaRota comoProposta() {
        return new AlternativaRota(chave, descricao, pontos, trechos, duracaoMinutos, saida, chegada,
            inicioAtendimento, fimAtendimento, esperaMinutos, tempoTotalMinutos, criterios, viavel, true);
    }

    public boolean usaTrecho(String chaveDoTrecho) {
        return trechos.contains(chaveDoTrecho);
    }

    public boolean passaPor(String codigoDoPonto) {
        return pontos.contains(codigoDoPonto);
    }

    public List<CriterioRota> getCriteriosNaoAtendidos() {
        return criterios.stream().filter(c -> !c.atendido()).toList();
    }
}
