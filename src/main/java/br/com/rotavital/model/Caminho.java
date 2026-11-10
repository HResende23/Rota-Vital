package br.com.rotavital.model;

import java.util.List;
import java.util.stream.Collectors;

/** Sequência de pontos e trechos do hemocentro até um hospital. */
public record Caminho(List<PontoDaMalha> pontos, List<Trecho> trechos) {
    public Caminho {
        pontos = List.copyOf(pontos);
        trechos = List.copyOf(trechos);
        if (pontos.size() != trechos.size() + 1) {
            throw new IllegalArgumentException("Caminho inconsistente");
        }
    }

    /** Identificador estável do caminho, por exemplo HEMOCENTRO>P1>AURORA. */
    public String getChave() {
        return pontos.stream().map(PontoDaMalha::codigo).collect(Collectors.joining(">"));
    }

    public String getDescricao() {
        return pontos.stream().map(PontoDaMalha::nome).collect(Collectors.joining(" → "));
    }

    public int duracao(boolean horarioDePico) {
        return trechos.stream().mapToInt(t -> t.duracao(horarioDePico)).sum();
    }
}
