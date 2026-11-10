package br.com.rotavital;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;
import br.com.rotavital.dto.ConfirmacaoReserva;
import br.com.rotavital.dto.NovaRequisicao;
import br.com.rotavital.model.*;
import br.com.rotavital.repository.*;
import br.com.rotavital.service.AlocacaoService;
import br.com.rotavital.service.MonitoramentoService;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Monitoramento iniciado pela reserva, leituras do sensor, alerta térmico e propriedade da remessa. */
@SpringBootTest
@WithMockUser(username = "solicitante@example.com")
@AutoConfigureMockMvc
class MonitoramentoIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AlocacaoService alocacao;
    @Autowired MonitoramentoService monitoramento;
    @Autowired RemessaRepository remessas;
    @Autowired RequisicaoRepository requisicoes;
    @Autowired BolsaHemoComponenteRepository bolsas;
    @Autowired UserRepository users;
    private Long usuarioId;

    @BeforeEach
    void limpar() {
        remessas.deleteAll();
        requisicoes.deleteAll();
        bolsas.deleteAll();
        users.deleteAll();
        usuarioId = users.saveAndFlush(new User("Solicitante", "solicitante@example.com")).getId();
    }

    private void bolsa(String codigo, TipoSanguineo tipo) {
        bolsas.saveAndFlush(new BolsaHemoComponente(codigo, TipoComponente.CONCENTRADO_HEMACIAS, tipo,
            LocalDate.now().plusDays(5), StatusBolsa.DISPONIVEL));
    }

    private Long requisicao(Long usuario, int quantidade) {
        return alocacao.cadastrar(new NovaRequisicao(usuario, "Hospital Aurora",
            TipoComponente.CONCENTRADO_HEMACIAS, TipoSanguineo.A_POSITIVO, quantidade)).id();
    }

    /** Reserva B101 e B102 pela API e devolve o id da remessa aberta pelo monitoramento. */
    private Long remessaReservada() throws Exception {
        bolsa("B101", TipoSanguineo.A_POSITIVO);
        bolsa("B102", TipoSanguineo.O_NEGATIVO);
        Long requisicaoId = requisicao(usuarioId, 2);
        mvc.perform(post("/api/v1/requisicoes/" + requisicaoId + "/reserva").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigosBolsas\":[\"B101\",\"B102\"]}"))
            .andExpect(status().isOk());
        return remessas.findByRequisicaoId(requisicaoId).orElseThrow().getId();
    }

    private ResultActions leitura(Long remessaId, String json) throws Exception {
        return mvc.perform(post("/api/v1/remessas/" + remessaId + "/leituras").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void confirmarReservaIniciaOMonitoramentoDaRemessa() throws Exception {
        Long id = remessaReservada();
        mvc.perform(get("/api/v1/remessas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("AGUARDANDO_ROTA"))
            .andExpect(jsonPath("$[0].codigo").value(startsWith("R-")));
        mvc.perform(get("/api/v1/remessas/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hospital").value("Hospital Aurora"))
            .andExpect(jsonPath("$.caixa").value("CAIXA_P"))
            .andExpect(jsonPath("$.faixa.minima").value(2.0))
            .andExpect(jsonPath("$.faixa.maxima").value(6.0))
            .andExpect(jsonPath("$.bolsas.length()").value(2))
            .andExpect(jsonPath("$.bolsas[0].status").value("RESERVADA"))
            .andExpect(jsonPath("$.leituras.length()").value(0))
            .andExpect(jsonPath("$.monitoramentoIniciadoEm").exists());
    }

    @Test
    void reservaRecusadaNaoAbreRemessaEMonitoramentoNaoDuplica() throws Exception {
        bolsa("INCOMPATIVEL", TipoSanguineo.B_POSITIVO);
        Long recusada = requisicao(usuarioId, 1);
        mvc.perform(post("/api/v1/requisicoes/" + recusada + "/reserva").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigosBolsas\":[\"INCOMPATIVEL\"]}"))
            .andExpect(status().isConflict());
        assertEquals(0, remessas.count());

        bolsa("B1", TipoSanguineo.A_POSITIVO);
        Long aceita = requisicao(usuarioId, 1);
        alocacao.confirmar(aceita, new ConfirmacaoReserva(List.of("B1")));
        var erro = assertThrows(ResponseStatusException.class, () -> monitoramento.iniciar(aceita));
        assertEquals(409, erro.getStatusCode().value());
        assertEquals(1, remessas.count());
    }

    @Test
    void segundaLeituraForaDaFaixaGeraAlertaEBloqueiaAsBolsasNoEstoque() throws Exception {
        Long id = remessaReservada();
        leitura(id, "{\"temperatura\":4.0}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ultimaLeitura.condicao").value("DENTRO_DA_FAIXA"))
            .andExpect(jsonPath("$.ultimaLeitura.conectividade").value("CONECTADA"));
        leitura(id, "{\"temperatura\":7.0,\"conectividade\":\"INSTAVEL\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ultimaLeitura.condicao").value("INICIO_DO_DESVIO"))
            .andExpect(jsonPath("$.status").value("AGUARDANDO_ROTA"))
            .andExpect(jsonPath("$.emDesvio").value(true))
            .andExpect(jsonPath("$.alerta").doesNotExist());
        leitura(id, "{\"temperatura\":8.0}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ultimaLeitura.condicao").value("ALERTA_TERMICO"))
            .andExpect(jsonPath("$.status").value("PENDENTE_AVALIACAO"))
            .andExpect(jsonPath("$.alerta.emitidoEm").exists())
            .andExpect(jsonPath("$.alerta.reconhecido").value(false))
            .andExpect(jsonPath("$.leituras.length()").value(3));
        assertEquals(StatusBolsa.INDISPONIVEL, bolsas.findById("B101").orElseThrow().getStatus());
        assertEquals(StatusBolsa.INDISPONIVEL, bolsas.findById("B102").orElseThrow().getStatus());
        mvc.perform(get("/api/v1/bolsas"))
            .andExpect(jsonPath("$[*].status", everyItem(is("INDISPONIVEL"))));

        mvc.perform(post("/api/v1/remessas/" + id + "/alerta/reconhecimento").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alerta.reconhecidoPor").value("solicitante@example.com"))
            .andExpect(jsonPath("$.status").value("PENDENTE_AVALIACAO"));
        mvc.perform(post("/api/v1/remessas/" + id + "/alerta/reconhecimento").with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("O alerta já foi reconhecido"));
        mvc.perform(post("/api/v1/remessas/" + id + "/transporte").with(csrf()))
            .andExpect(status().isConflict());
        assertEquals(StatusBolsa.INDISPONIVEL, bolsas.findById("B101").orElseThrow().getStatus(),
            "reconhecer o alerta não libera as bolsas");
    }

    @Test
    void reconhecerSemAlertaEhConflito() throws Exception {
        Long id = remessaReservada();
        mvc.perform(post("/api/v1/remessas/" + id + "/alerta/reconhecimento").with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("Não há alerta térmico para reconhecer"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"temperatura\":null}", "{\"temperatura\":60.1}",
        "{\"temperatura\":-50.1}", "{\"temperatura\":\"quente\"}",
        "{\"temperatura\":4.0,\"conectividade\":\"WIFI\"}", "{"})
    void rejeitaLeituraInvalidaSemRegistrar(String json) throws Exception {
        Long id = remessaReservada();
        leitura(id, json).andExpect(status().isBadRequest());
        assertTrue(monitoramento.consultar(id).leituras().isEmpty());
    }

    @Test
    void naoExpoeNemAlteraRemessaDeOutroUsuario() throws Exception {
        var outro = users.saveAndFlush(new User("Outro", "outro@example.com"));
        bolsa("B9", TipoSanguineo.A_POSITIVO);
        Long requisicaoDoOutro = requisicao(outro.getId(), 1);
        alocacao.confirmar(requisicaoDoOutro, new ConfirmacaoReserva(List.of("B9")));
        Long id = remessas.findByRequisicaoId(requisicaoDoOutro).orElseThrow().getId();

        mvc.perform(get("/api/v1/remessas")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/remessas/" + id)).andExpect(status().isForbidden());
        leitura(id, "{\"temperatura\":4.0}").andExpect(status().isForbidden());
        for (String acao : List.of("/alerta/reconhecimento", "/transporte", "/entrega")) {
            mvc.perform(post("/api/v1/remessas/" + id + acao).with(csrf())).andExpect(status().isForbidden());
        }
        assertTrue(monitoramento.consultar(id).leituras().isEmpty());
        assertEquals(StatusRemessa.AGUARDANDO_ROTA, monitoramento.consultar(id).status());
    }

    @Test
    void remessaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/v1/remessas/" + Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Remessa não encontrada"));
        leitura(Long.MAX_VALUE, "{\"temperatura\":4.0}").andExpect(status().isNotFound());
    }

    @Test
    void mutacoesExigemCsrf() throws Exception {
        Long id = remessaReservada();
        mvc.perform(post("/api/v1/remessas/" + id + "/leituras")
                .contentType(MediaType.APPLICATION_JSON).content("{\"temperatura\":4.0}"))
            .andExpect(status().isForbidden());
        assertTrue(monitoramento.consultar(id).leituras().isEmpty());
    }

    @Test
    @WithAnonymousUser
    void apiExigeAutenticacao() throws Exception {
        mvc.perform(get("/api/v1/remessas")).andExpect(status().isUnauthorized());
    }
}
