package br.com.rotavital.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import br.com.rotavital.dto.AlternativaRota;
import br.com.rotavital.dto.CriterioRota;
import br.com.rotavital.model.Caminho;
import br.com.rotavital.model.ModeloCaixa;
import br.com.rotavital.model.PontoDaMalha;
import br.com.rotavital.model.Trecho;

/**
 * Regras de viabilidade de um caminho: janela de recebimento do hospital, capacidade da caixa,
 * autonomia térmica e validade das bolsas na entrega. Não depende de banco nem do Spring.
 */
public final class ViabilidadeDeRota {
    /** Tempo de descarga e conferência das bolsas no hospital. */
    public static final int MINUTOS_DE_ATENDIMENTO = 5;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private ViabilidadeDeRota() {}

    /** O que a remessa transporta e em qual caixa. */
    public record Carga(int bolsas, ModeloCaixa caixa, int quantidadeCaixas,
                        String bolsaMaisProximaDoVencimento, LocalDate menorValidade) {
        public int capacidade() { return caixa.getCapacidade() * quantidadeCaixas; }
        public int autonomiaMinutos() { return caixa.getAutonomiaMinutos(); }

        String descricaoDaCaixa() {
            return quantidadeCaixas == 1 ? caixa.getRotulo() : quantidadeCaixas + " × " + caixa.getRotulo();
        }
    }

    public static AlternativaRota avaliar(Caminho caminho, LocalDateTime saida, LocalDateTime janelaInicio,
                                          LocalDateTime janelaFim, boolean horarioDePico, Carga carga) {
        int duracao = caminho.duracao(horarioDePico);
        LocalDateTime chegada = saida.plusMinutes(duracao);
        // Chegando antes da janela, a equipe aguarda a abertura para iniciar o atendimento.
        LocalDateTime inicioAtendimento = chegada.isBefore(janelaInicio) ? janelaInicio : chegada;
        LocalDateTime fimAtendimento = inicioAtendimento.plusMinutes(MINUTOS_DE_ATENDIMENTO);
        int espera = (int) Duration.between(chegada, inicioAtendimento).toMinutes();
        int tempoTotal = (int) Duration.between(saida, fimAtendimento).toMinutes();

        List<CriterioRota> criterios = List.of(
            janela(saida, chegada, inicioAtendimento, fimAtendimento, janelaInicio, janelaFim, espera),
            capacidade(carga),
            autonomia(tempoTotal, carga),
            validade(saida, fimAtendimento, carga));
        boolean viavel = criterios.stream().allMatch(CriterioRota::atendido);
        return new AlternativaRota(caminho.getChave(), caminho.getDescricao(),
            caminho.pontos().stream().map(PontoDaMalha::codigo).toList(),
            caminho.trechos().stream().map(Trecho::getChave).toList(),
            duracao, saida, chegada, inicioAtendimento, fimAtendimento, espera, tempoTotal,
            criterios, viavel, false);
    }

    private static CriterioRota janela(LocalDateTime saida, LocalDateTime chegada, LocalDateTime inicio,
                                       LocalDateTime fim, LocalDateTime janelaInicio,
                                       LocalDateTime janelaFim, int espera) {
        String janela = hora(janelaInicio, saida) + "–" + hora(janelaFim, saida);
        if (fim.isAfter(janelaFim)) {
            return new CriterioRota("Janela de recebimento", false,
                "Atendimento terminaria às " + hora(fim, saida) + ", depois da janela " + janela);
        }
        String atendimento = "atendimento " + hora(inicio, saida) + "–" + hora(fim, saida) + " na janela " + janela;
        return new CriterioRota("Janela de recebimento", true, espera > 0
            ? "Chega às " + hora(chegada, saida) + " e aguarda " + espera + " min; " + atendimento
            : "Chega às " + hora(chegada, saida) + "; " + atendimento);
    }

    private static CriterioRota capacidade(Carga carga) {
        return new CriterioRota("Capacidade da caixa", carga.bolsas() <= carga.capacidade(),
            carga.bolsas() + " de " + carga.capacidade() + " bolsas · " + carga.descricaoDaCaixa());
    }

    private static CriterioRota autonomia(int tempoTotal, Carga carga) {
        boolean atende = tempoTotal <= carga.autonomiaMinutos();
        return new CriterioRota("Autonomia térmica", atende, atende
            ? "Tempo total de " + tempoTotal + " min dentro da autonomia de " + carga.autonomiaMinutos() + " min"
            : "Tempo total de " + tempoTotal + " min excede a autonomia de " + carga.autonomiaMinutos() + " min");
    }

    private static CriterioRota validade(LocalDateTime saida, LocalDateTime fimAtendimento, Carga carga) {
        if (carga.menorValidade() == null) {
            return new CriterioRota("Validade na entrega", true, "Sem bolsas a verificar");
        }
        LocalDate entrega = fimAtendimento.toLocalDate();
        boolean atende = !carga.menorValidade().isBefore(entrega);
        return new CriterioRota("Validade na entrega", atende, atende
            ? "Bolsas válidas na entrega; a próxima a vencer é " + carga.bolsaMaisProximaDoVencimento()
                + " (" + carga.menorValidade().format(DIA) + ")"
            : "Bolsa " + carga.bolsaMaisProximaDoVencimento() + " vence em "
                + carga.menorValidade().format(DIA) + ", antes da entrega em " + entrega.format(DIA));
    }

    /** Mostra só a hora quando o horário cai no mesmo dia da saída. */
    private static String hora(LocalDateTime horario, LocalDateTime referencia) {
        return horario.toLocalDate().equals(referencia.toLocalDate())
            ? horario.format(HORA) : horario.format(DIA_HORA);
    }
}
