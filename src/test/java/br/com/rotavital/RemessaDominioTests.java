package br.com.rotavital;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import br.com.rotavital.model.*;
import static br.com.rotavital.model.CondicaoLeitura.*;
import static br.com.rotavital.model.StatusRemessa.*;
import static org.junit.jupiter.api.Assertions.*;

/** Regras da remessa sem banco: abertura na reserva, leituras, alerta e ciclo de entrega. */
class RemessaDominioTests {
    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 1, 10, 9, 0);

    private Requisicao alocada(TipoComponente componente, int quantidade) {
        var requisicao = new Requisicao(new User("Ana", "ana@example.com"), "Hospital Aurora",
            componente, TipoSanguineo.A_POSITIVO, quantidade);
        List<BolsaHemoComponente> bolsas = new ArrayList<>();
        for (int i = 1; i <= quantidade; i++) {
            var bolsa = new BolsaHemoComponente("B" + i, componente, TipoSanguineo.A_POSITIVO,
                LocalDate.now().plusDays(5), StatusBolsa.DISPONIVEL);
            bolsa.reservar();
            bolsas.add(bolsa);
        }
        requisicao.alocar(bolsas);
        return requisicao;
    }

    private Remessa remessa() {
        return Remessa.iniciarMonitoramento(alocada(TipoComponente.CONCENTRADO_HEMACIAS, 2), INICIO);
    }

    private PlanoDeRota plano() {
        return new PlanoDeRota("AURORA", "Hospital Aurora", "HEMOCENTRO>AURORA",
            "Hemocentro Central → Hospital Aurora", 40, INICIO.plusMinutes(30), INICIO.plusMinutes(70),
            INICIO.plusMinutes(70), INICIO.plusMinutes(75), INICIO.plusMinutes(60), INICIO.plusMinutes(120),
            45, false, INICIO.plusMinutes(5));
    }

    @Test
    void monitoramentoSoComecaAposAReserva() {
        var pendente = new Requisicao(new User("Ana", "ana@example.com"), "Hospital Aurora",
            TipoComponente.PLASMA, TipoSanguineo.A_POSITIVO, 1);
        assertThrows(IllegalStateException.class, () -> Remessa.iniciarMonitoramento(pendente, INICIO));
        var remessa = remessa();
        assertEquals(AGUARDANDO_ROTA, remessa.getStatus());
        assertEquals(INICIO, remessa.getMonitoramentoIniciadoEm());
        assertTrue(remessa.getLeituras().isEmpty());
        assertNull(remessa.getPlano());
    }

    @ParameterizedTest
    @CsvSource({"1,CAIXA_P,1", "4,CAIXA_P,1", "5,CAIXA_M,1", "8,CAIXA_M,1", "9,CAIXA_G,1",
        "16,CAIXA_G,1", "17,CAIXA_G,2", "40,CAIXA_G,3"})
    void escolheAMenorCaixaQueComportaAsBolsas(int bolsas, ModeloCaixa esperada, int caixas) {
        assertEquals(esperada, ModeloCaixa.para(bolsas));
        assertEquals(caixas, ModeloCaixa.para(bolsas).caixasNecessarias(bolsas));
    }

    @Test
    void faixaTermicaVemDoComponente() {
        assertEquals(new FaixaTermica(2.0, 6.0), remessa().getFaixa());
        var plasma = Remessa.iniciarMonitoramento(alocada(TipoComponente.PLASMA, 1), INICIO);
        assertEquals(new FaixaTermica(-30.0, -18.0), plasma.getFaixa());
        var plaquetas = Remessa.iniciarMonitoramento(alocada(TipoComponente.PLAQUETAS, 1), INICIO);
        assertTrue(plaquetas.getFaixa().contem(22.0));
        assertEquals("2,0 a 6,0 °C", remessa().getFaixa().getRotulo());
        assertThrows(IllegalArgumentException.class, () -> new FaixaTermica(6.0, 2.0));
    }

    @Test
    void segundaLeituraForaDaFaixaConfirmaAlertaEBloqueiaBolsas() {
        var remessa = remessa();
        assertEquals(DENTRO_DA_FAIXA, remessa.registrarLeitura(4.0, Conectividade.CONECTADA, INICIO.plusMinutes(60)).getCondicao());
        var primeira = remessa.registrarLeitura(7.0, Conectividade.CONECTADA, INICIO.plusMinutes(63));
        assertEquals(INICIO_DO_DESVIO, primeira.getCondicao());
        assertEquals(AGUARDANDO_ROTA, remessa.getStatus());
        assertFalse(remessa.isEmAlerta());
        assertEquals(StatusBolsa.RESERVADA, remessa.getRequisicao().getBolsas().get(0).getStatus());

        var segunda = remessa.registrarLeitura(8.0, Conectividade.CONECTADA, INICIO.plusMinutes(65));
        assertEquals(ALERTA_TERMICO, segunda.getCondicao());
        assertEquals(PENDENTE_AVALIACAO, remessa.getStatus());
        assertEquals(INICIO.plusMinutes(63), remessa.getAlertaDesvioIniciadoEm());
        assertEquals(INICIO.plusMinutes(65), remessa.getAlertaEmitidoEm());
        remessa.getRequisicao().getBolsas()
            .forEach(b -> assertEquals(StatusBolsa.INDISPONIVEL, b.getStatus()));
    }

    @Test
    void leituraIsoladaForaDaFaixaNaoGeraAlerta() {
        var remessa = remessa();
        remessa.registrarLeitura(1.5, Conectividade.INSTAVEL, INICIO.plusMinutes(1));
        assertEquals(INICIO.plusMinutes(1), remessa.getDesvioIniciadoEm());
        remessa.registrarLeitura(3.0, null, INICIO.plusMinutes(2));
        assertNull(remessa.getDesvioIniciadoEm());
        assertFalse(remessa.isEmAlerta());
        assertEquals(Conectividade.CONECTADA, remessa.getUltimaLeitura().getConectividade());
        assertEquals(AGUARDANDO_ROTA, remessa.getStatus());
    }

    @Test
    void voltarAFaixaEncerraODesvioMasNaoDesfazOAlerta() {
        var remessa = remessa();
        remessa.registrarLeitura(7.0, Conectividade.CONECTADA, INICIO.plusMinutes(1));
        remessa.registrarLeitura(7.5, Conectividade.CONECTADA, INICIO.plusMinutes(2));
        remessa.registrarLeitura(4.0, Conectividade.CONECTADA, INICIO.plusMinutes(3));
        assertNull(remessa.getDesvioIniciadoEm());
        assertTrue(remessa.isEmAlerta());
        assertEquals(PENDENTE_AVALIACAO, remessa.getStatus());
        remessa.registrarLeitura(9.0, Conectividade.CONECTADA, INICIO.plusMinutes(4));
        remessa.registrarLeitura(9.5, Conectividade.CONECTADA, INICIO.plusMinutes(5));
        assertEquals(INICIO.plusMinutes(2), remessa.getAlertaEmitidoEm(), "o primeiro alerta é preservado");
        assertEquals(5, remessa.getLeituras().size());
    }

    @Test
    void reconhecerAlertaRegistraCienciaSemLiberarBolsas() {
        var remessa = remessa();
        assertThrows(IllegalStateException.class, () -> remessa.reconhecerAlerta("ana@example.com", INICIO));
        remessa.registrarLeitura(10.0, Conectividade.CONECTADA, INICIO.plusMinutes(1));
        remessa.registrarLeitura(11.0, Conectividade.CONECTADA, INICIO.plusMinutes(2));
        remessa.reconhecerAlerta("ana@example.com", INICIO.plusMinutes(3));
        assertEquals("ana@example.com", remessa.getAlertaReconhecidoPor());
        assertEquals(INICIO.plusMinutes(3), remessa.getAlertaReconhecidoEm());
        assertThrows(IllegalStateException.class, () -> remessa.reconhecerAlerta("ana@example.com", INICIO));
        assertEquals(PENDENTE_AVALIACAO, remessa.getStatus());
        remessa.getRequisicao().getBolsas()
            .forEach(b -> assertEquals(StatusBolsa.INDISPONIVEL, b.getStatus()));
    }

    @Test
    void remessaEmAlertaNaoPodeSerPlanejadaNemSair() {
        var remessa = remessa();
        remessa.planejar(plano());
        remessa.registrarLeitura(10.0, Conectividade.CONECTADA, INICIO.plusMinutes(1));
        remessa.registrarLeitura(10.0, Conectividade.CONECTADA, INICIO.plusMinutes(2));
        var erro = assertThrows(IllegalStateException.class, () -> remessa.planejar(plano()));
        assertTrue(erro.getMessage().contains("alerta térmico"));
        assertThrows(IllegalStateException.class, () -> remessa.iniciarTransporte(INICIO.plusMinutes(3)));
    }

    @Test
    void cicloDaReservaAteAEntrega() {
        var remessa = remessa();
        var semPlano = assertThrows(IllegalStateException.class, () -> remessa.iniciarTransporte(INICIO));
        assertTrue(semPlano.getMessage().contains("Planeje a rota"));
        assertThrows(IllegalStateException.class, () -> remessa.registrarEntrega(INICIO));

        remessa.planejar(plano());
        assertEquals(PLANEJADA, remessa.getStatus());
        remessa.planejar(plano());
        assertEquals(PLANEJADA, remessa.getStatus(), "pode replanejar antes da saída");

        remessa.iniciarTransporte(INICIO.plusMinutes(30));
        assertEquals(EM_TRANSPORTE, remessa.getStatus());
        assertEquals(INICIO.plusMinutes(30), remessa.getSaidaEm());
        assertThrows(IllegalStateException.class, () -> remessa.planejar(plano()));
        remessa.registrarLeitura(4.5, Conectividade.CONECTADA, INICIO.plusMinutes(50));

        remessa.registrarEntrega(INICIO.plusMinutes(75));
        assertEquals(ENTREGUE, remessa.getStatus());
        assertEquals(INICIO.plusMinutes(75), remessa.getMonitoramentoEncerradoEm());
        var encerrado = assertThrows(IllegalStateException.class,
            () -> remessa.registrarLeitura(4.0, Conectividade.CONECTADA, INICIO.plusMinutes(80)));
        assertTrue(encerrado.getMessage().contains("encerrado"));
        assertThrows(IllegalStateException.class, () -> remessa.registrarEntrega(INICIO.plusMinutes(90)));
    }

    @Test
    void leituraNaoPodeVoltarNoTempoNemTerValorInvalido() {
        var remessa = remessa();
        remessa.registrarLeitura(4.0, Conectividade.CONECTADA, INICIO.plusMinutes(10));
        assertThrows(IllegalStateException.class,
            () -> remessa.registrarLeitura(4.0, Conectividade.CONECTADA, INICIO.plusMinutes(9)));
        assertThrows(IllegalArgumentException.class,
            () -> remessa.registrarLeitura(Double.NaN, Conectividade.CONECTADA, INICIO.plusMinutes(11)));
        assertEquals(1, remessa.getLeituras().size());
        assertThrows(UnsupportedOperationException.class, () -> remessa.getLeituras().clear());
    }
}
