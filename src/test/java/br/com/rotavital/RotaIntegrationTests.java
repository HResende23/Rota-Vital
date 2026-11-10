package br.com.rotavital;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import br.com.rotavital.dto.ConfirmacaoReserva;
import br.com.rotavital.dto.NovaRequisicao;
import br.com.rotavital.model.*;
import br.com.rotavital.repository.*;
import br.com.rotavital.service.AlocacaoService;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Planejamento de rota pela API: simulação, confirmação, saída e entrega da remessa. */
@SpringBootTest
@WithMockUser(username = "solicitante@example.com")
@AutoConfigureMockMvc
class RotaIntegrationTests {
    private static final LocalDate AMANHA = LocalDate.now().plusDays(1);

    @Autowired MockMvc mvc;
    @Autowired AlocacaoService alocacao;
    @Autowired RemessaRepository remessas;
    @Autowired RequisicaoRepository requisicoes;
    @Autowired BolsaHemoComponenteRepository bolsas;
    @Autowired UserRepository users;
    private Long remessaId;

    @BeforeEach
    void reservar() {
        remessas.deleteAll();
        requisicoes.deleteAll();
        bolsas.deleteAll();
        users.deleteAll();
        remessaId = remessaCom(5);
    }

    /** Requisição de 2 bolsas do Hospital Aurora reservada; a validade é relativa a hoje. */
    private Long remessaCom(int diasDeValidade) {
        Long usuario = users.findByEmail("solicitante@example.com")
            .orElseGet(() -> users.saveAndFlush(new User("Solicitante", "solicitante@example.com"))).getId();
        String prefixo = "V" + diasDeValidade + "-";
        for (String codigo : List.of(prefixo + "1", prefixo + "2")) {
            bolsas.saveAndFlush(new BolsaHemoComponente(codigo, TipoComponente.CONCENTRADO_HEMACIAS,
                TipoSanguineo.A_POSITIVO, LocalDate.now().plusDays(diasDeValidade), StatusBolsa.DISPONIVEL));
        }
        Long requisicao = alocacao.cadastrar(new NovaRequisicao(usuario, "Hospital Aurora",
            TipoComponente.CONCENTRADO_HEMACIAS, TipoSanguineo.A_POSITIVO, 2)).id();
        alocacao.confirmar(requisicao, new ConfirmacaoReserva(List.of(prefixo + "1", prefixo + "2")));
        return remessas.findByRequisicaoId(requisicao).orElseThrow().getId();
    }

    private ResultActions simular(Long id, String destino, LocalDateTime saida, String inicio, String fim)
            throws Exception {
        return mvc.perform(get("/api/v1/remessas/" + id + "/rota/alternativas")
            .param("destino", destino).param("saida", saida.toString())
            .param("janelaInicio", inicio).param("janelaFim", fim));
    }

    private ResultActions confirmar(Long id, String json) throws Exception {
        return mvc.perform(post("/api/v1/remessas/" + id + "/rota").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String corpo(String destino, LocalDateTime saida, String inicio, String fim, String caminho) {
        return "{\"destino\":\"" + destino + "\",\"saida\":\"" + saida + "\",\"janelaInicio\":\"" + inicio
            + "\",\"janelaFim\":\"" + fim + "\"" + (caminho == null ? "" : ",\"caminho\":\"" + caminho + "\"") + "}";
    }

    @Test
    void foraDoPicoPropoeOCaminhoDiretoSemAlterarARemessa() throws Exception {
        simular(remessaId, "AURORA", AMANHA.atTime(9, 30), "10:00", "11:00")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.horarioDePico").value(false))
            .andExpect(jsonPath("$.destino.nome").value("Hospital Aurora"))
            .andExpect(jsonPath("$.alternativas.length()").value(3))
            .andExpect(jsonPath("$.alternativas[0].chave").value("HEMOCENTRO>AURORA"))
            .andExpect(jsonPath("$.proposta.chave").value("HEMOCENTRO>AURORA"))
            .andExpect(jsonPath("$.proposta.duracaoMinutos").value(40))
            .andExpect(jsonPath("$.proposta.tempoTotalMinutos").value(45))
            .andExpect(jsonPath("$.proposta.chegada").value(startsWith(AMANHA + "T10:10")))
            .andExpect(jsonPath("$.proposta.criterios[*].atendido", everyItem(is(true))));
        assertEquals(StatusRemessa.AGUARDANDO_ROTA, remessas.findById(remessaId).orElseThrow().getStatus());
    }

    @Test
    void noHorarioDePicoPropoeAViaExpressa() throws Exception {
        simular(remessaId, "AURORA", AMANHA.atTime(8, 0), "08:30", "10:00")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.horarioDePico").value(true))
            .andExpect(jsonPath("$.proposta.chave").value("HEMOCENTRO>P1>AURORA"))
            .andExpect(jsonPath("$.alternativas[1].chave").value("HEMOCENTRO>AURORA"))
            .andExpect(jsonPath("$.alternativas[1].duracaoMinutos").value(60));
    }

    @Test
    void confirmarPlanejamentoLiberaSaidaEEntrega() throws Exception {
        confirmar(remessaId, corpo("AURORA", AMANHA.atTime(9, 30), "10:00", "11:00", null))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PLANEJADA"))
            .andExpect(jsonPath("$.plano.caminhoChave").value("HEMOCENTRO>AURORA"))
            .andExpect(jsonPath("$.plano.destinoNome").value("Hospital Aurora"))
            .andExpect(jsonPath("$.plano.chegadaPrevista").value(startsWith(AMANHA + "T10:10")))
            .andExpect(jsonPath("$.plano.tempoTotalMinutos").value(45));
        mvc.perform(post("/api/v1/remessas/" + remessaId + "/transporte").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_TRANSPORTE"))
            .andExpect(jsonPath("$.saidaEm").exists());
        confirmar(remessaId, corpo("AURORA", AMANHA.atTime(9, 30), "10:00", "11:00", null))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("A remessa já saiu do hemocentro"));
        mvc.perform(post("/api/v1/remessas/" + remessaId + "/leituras").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"temperatura\":4.5}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/remessas/" + remessaId + "/entrega").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENTREGUE"))
            .andExpect(jsonPath("$.monitoramentoEncerradoEm").exists());
        mvc.perform(post("/api/v1/remessas/" + remessaId + "/leituras").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"temperatura\":4.5}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value(containsString("encerrado")));
    }

    @Test
    void transporteSemRotaPlanejadaEhRecusado() throws Exception {
        mvc.perform(post("/api/v1/remessas/" + remessaId + "/transporte").with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("Planeje a rota antes de iniciar o transporte"));
        mvc.perform(post("/api/v1/remessas/" + remessaId + "/entrega").with(csrf()))
            .andExpect(status().isConflict());
    }

    @Test
    void planejadorPodeEscolherOutraAlternativaViavel() throws Exception {
        confirmar(remessaId, corpo("AURORA", AMANHA.atTime(9, 30), "10:00", "11:00", "HEMOCENTRO>P1>AURORA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plano.caminhoChave").value("HEMOCENTRO>P1>AURORA"))
            .andExpect(jsonPath("$.plano.duracaoMinutos").value(50));
    }

    @Test
    void recusaCaminhoInviavelOuForaDasAlternativas() throws Exception {
        // Saindo às 10:40, nem o caminho mais curto termina o atendimento até 11:00.
        simular(remessaId, "AURORA", AMANHA.atTime(10, 40), "10:00", "11:00")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.proposta").doesNotExist())
            .andExpect(jsonPath("$.alternativas[*].viavel", everyItem(is(false))));
        confirmar(remessaId, corpo("AURORA", AMANHA.atTime(10, 40), "10:00", "11:00", null))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value(containsString("janela de recebimento")));
        confirmar(remessaId, corpo("AURORA", AMANHA.atTime(9, 30), "10:00", "11:00", "HEMOCENTRO>P9>AURORA"))
            .andExpect(status().isBadRequest());
        assertEquals(StatusRemessa.AGUARDANDO_ROTA, remessas.findById(remessaId).orElseThrow().getStatus());
    }

    @Test
    void bolsaQueVenceAntesDaEntregaImpedeOPlanejamento() throws Exception {
        Long venceHoje = remessaCom(0);
        simular(venceHoje, "AURORA", AMANHA.atTime(9, 30), "10:00", "11:00")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.proposta").doesNotExist())
            .andExpect(jsonPath("$.alternativas[0].criteriosNaoAtendidos[0].nome").value("Validade na entrega"));
    }

    @Test
    void validaParametrosDoPlanejamento() throws Exception {
        simular(remessaId, "AURORA", AMANHA.atTime(9, 30), "11:00", "10:00")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("A janela de recebimento deve terminar depois de começar"));
        simular(remessaId, "AURORA", LocalDateTime.now().minusHours(2), "10:00", "11:00")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("A saída prevista não pode estar no passado"));
        for (String destino : List.of("P1", "HEMOCENTRO", "NAO_EXISTE")) {
            simular(remessaId, destino, AMANHA.atTime(9, 30), "10:00", "11:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Destino fora da malha sintética"));
        }
        confirmar(remessaId, "{\"destino\":\"AURORA\"}").andExpect(status().isBadRequest());
        confirmar(remessaId, "{\"destino\":\" \",\"saida\":\"" + AMANHA.atTime(9, 30)
            + "\",\"janelaInicio\":\"10:00\",\"janelaFim\":\"11:00\"}").andExpect(status().isBadRequest());
    }

    @Test
    void remessaEmAlertaNaoPodeSerPlanejada() throws Exception {
        for (String temperatura : List.of("9.0", "9.5")) {
            mvc.perform(post("/api/v1/remessas/" + remessaId + "/leituras").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"temperatura\":" + temperatura + "}"))
                .andExpect(status().isCreated());
        }
        simular(remessaId, "AURORA", AMANHA.atTime(9, 30), "10:00", "11:00")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value(containsString("alerta térmico")));
    }
}
