package br.com.rotavital;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import br.com.rotavital.dto.AlternativaRota;
import br.com.rotavital.dto.CriterioRota;
import br.com.rotavital.model.Caminho;
import br.com.rotavital.model.ModeloCaixa;
import br.com.rotavital.service.MalhaViaria;
import br.com.rotavital.service.ViabilidadeDeRota;
import br.com.rotavital.service.ViabilidadeDeRota.Carga;
import static org.junit.jupiter.api.Assertions.*;

/** Critérios de viabilidade sem banco: janela, capacidade, autonomia e validade. */
class ViabilidadeDeRotaTests {
    private static final LocalDate DIA = LocalDate.of(2030, 1, 10);
    private final Caminho direto = new MalhaViaria().caminhos(MalhaViaria.HEMOCENTRO, "AURORA", 1).get(0);
    private final Carga duasBolsas = new Carga(2, ModeloCaixa.CAIXA_P, 1, "B101", DIA.plusDays(3));

    private AlternativaRota avaliar(String saida, Carga carga) {
        return ViabilidadeDeRota.avaliar(direto, DIA.atTime(java.time.LocalTime.parse(saida)),
            DIA.atTime(10, 0), DIA.atTime(11, 0), false, carga);
    }

    private CriterioRota criterio(AlternativaRota alternativa, String nome) {
        return alternativa.criterios().stream().filter(c -> c.nome().equals(nome)).findFirst().orElseThrow();
    }

    @Test
    void cenarioDoPrototipoEhViavel() {
        // Saída 09:30, 40 min de caminho direto, janela 10:00–11:00 e caixa P com 2 bolsas.
        var alternativa = avaliar("09:30", duasBolsas);
        assertTrue(alternativa.viavel());
        assertEquals(DIA.atTime(10, 10), alternativa.chegada());
        assertEquals(DIA.atTime(10, 10), alternativa.inicioAtendimento());
        assertEquals(DIA.atTime(10, 15), alternativa.fimAtendimento());
        assertEquals(0, alternativa.esperaMinutos());
        assertEquals(45, alternativa.tempoTotalMinutos());
        assertEquals("2 de 4 bolsas · Caixa térmica P", criterio(alternativa, "Capacidade da caixa").detalhe());
        assertEquals(4, alternativa.criterios().size());
    }

    @Test
    void chegarAntesDaJanelaGeraEsperaQueContaNaAutonomia() {
        var alternativa = avaliar("09:00", duasBolsas);
        assertEquals(20, alternativa.esperaMinutos());
        assertEquals(DIA.atTime(10, 0), alternativa.inicioAtendimento());
        assertEquals(65, alternativa.tempoTotalMinutos());
        assertTrue(alternativa.viavel());

        var cedoDemais = avaliar("07:00", duasBolsas);
        assertFalse(cedoDemais.viavel());
        assertTrue(criterio(cedoDemais, "Janela de recebimento").atendido());
        assertFalse(criterio(cedoDemais, "Autonomia térmica").atendido());
        assertEquals(185, cedoDemais.tempoTotalMinutos());
    }

    @Test
    void atendimentoQueTerminaDepoisDaJanelaEhInviavel() {
        var noLimite = avaliar("10:15", duasBolsas);
        assertEquals(DIA.atTime(11, 0), noLimite.fimAtendimento());
        assertTrue(noLimite.viavel(), "terminar exatamente no fim da janela é aceito");

        var atrasada = avaliar("10:16", duasBolsas);
        assertFalse(atrasada.viavel());
        assertFalse(criterio(atrasada, "Janela de recebimento").atendido());
        assertEquals(1, atrasada.getCriteriosNaoAtendidos().size());
    }

    @Test
    void bolsaQueVenceAntesDaEntregaTornaOCaminhoInviavel() {
        var venceOntem = new Carga(2, ModeloCaixa.CAIXA_P, 1, "B7", DIA.minusDays(1));
        var alternativa = avaliar("09:30", venceOntem);
        assertFalse(alternativa.viavel());
        assertEquals("Bolsa B7 vence em 09/01, antes da entrega em 10/01",
            criterio(alternativa, "Validade na entrega").detalhe());
        assertTrue(avaliar("09:30", new Carga(2, ModeloCaixa.CAIXA_P, 1, "B7", DIA)).viavel(),
            "bolsa que vence no dia da entrega ainda é válida");
    }

    @Test
    void cargaAcimaDaCapacidadeEhInviavel() {
        var excesso = new Carga(5, ModeloCaixa.CAIXA_P, 1, "B1", DIA.plusDays(3));
        assertFalse(criterio(avaliar("09:30", excesso), "Capacidade da caixa").atendido());
        var duasCaixas = new Carga(20, ModeloCaixa.CAIXA_G, 2, "B1", DIA.plusDays(3));
        assertEquals("20 de 32 bolsas · 2 × Caixa térmica G",
            criterio(avaliar("09:30", duasCaixas), "Capacidade da caixa").detalhe());
    }

    @Test
    void janelaNoDiaSeguinteMostraAData() {
        var alternativa = ViabilidadeDeRota.avaliar(direto, DIA.atTime(23, 50),
            DIA.plusDays(1).atTime(0, 0), DIA.plusDays(1).atTime(1, 0), false, duasBolsas);
        assertEquals(LocalDateTime.of(DIA.plusDays(1), java.time.LocalTime.of(0, 30)), alternativa.chegada());
        assertTrue(criterio(alternativa, "Janela de recebimento").detalhe().contains("11/01 00:30"));
    }
}
