package br.com.rotavital;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import br.com.rotavital.dto.*;
import br.com.rotavital.model.*;
import br.com.rotavital.repository.*;
import br.com.rotavital.service.AlocacaoService;
import static br.com.rotavital.model.TipoComponente.*;
import static br.com.rotavital.model.TipoSanguineo.*;
import static br.com.rotavital.model.StatusBolsa.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AlocacaoIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AlocacaoService service;
    @Autowired BolsaHemoComponenteRepository bolsas;
    @Autowired RequisicaoRepository requisicoes;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpar() {
        requisicoes.deleteAll();
        bolsas.deleteAll();
    }

    private Long requisicao(int quantidade) {
        return service.cadastrar(new NovaRequisicao("Hospital Aurora", CONCENTRADO_HEMACIAS,
            A_POSITIVO, quantidade)).id();
    }

    private void bolsa(String codigo, TipoComponente componente, TipoSanguineo tipo,
                       int dias, StatusBolsa status) {
        bolsas.saveAndFlush(new BolsaHemoComponente(codigo, componente, tipo,
            LocalDate.now().plusDays(dias), status));
    }

    private void bolsa(String codigo, int dias) {
        bolsa(codigo, CONCENTRADO_HEMACIAS, A_POSITIVO, dias, DISPONIVEL);
    }

    private String url(Long id) { return "/api/v1/requisicoes/" + id; }

    @Test
    void propostaFiltraAntesDoFefoSemReservar() throws Exception {
        Long id = requisicao(2);
        bolsa("B103", 5);
        bolsa("B102", 2);
        bolsa("B101", CONCENTRADO_HEMACIAS, O_NEGATIVO, 1, DISPONIVEL);
        bolsa("INCOMPATIVEL", CONCENTRADO_HEMACIAS, B_POSITIVO, 0, DISPONIVEL);
        bolsa("RESERVADA", CONCENTRADO_HEMACIAS, A_POSITIVO, 0, RESERVADA);
        bolsa("BLOQUEADA", CONCENTRADO_HEMACIAS, A_POSITIVO, 0, INDISPONIVEL);
        bolsa("PLASMA", PLASMA, A_POSITIVO, 0, DISPONIVEL);
        bolsa("VENCIDA", 0);
        jdbc.update("update bolsa_hemo_componente set validade = ? where codigo = ?",
            LocalDate.now().minusDays(1), "VENCIDA");
        mvc.perform(get(url(id) + "/proposta-alocacao"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bolsas[0].codigo").value("B101"))
            .andExpect(jsonPath("$.bolsas[1].codigo").value("B102"))
            .andExpect(jsonPath("$.faltantes").value(0));
        assertEquals(DISPONIVEL, bolsas.findById("B101").orElseThrow().getStatus());
        assertEquals(StatusRequisicao.PENDENTE_ALOCACAO, service.consultar(id).status());
    }

    @Test
    void desempataPorCodigoEAceitaValidadeHoje() {
        Long id = requisicao(1);
        bolsa("B2", 0); bolsa("B1", 0);
        assertEquals("B1", service.propor(id).bolsas().get(0).getCodigo());
    }

    @Test
    void informaFaltantesInclusiveEstoqueVazio() throws Exception {
        Long id = requisicao(2);
        mvc.perform(get(url(id) + "/proposta-alocacao"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.faltantes").value(2));
        bolsa("B1", 1);
        mvc.perform(get(url(id) + "/proposta-alocacao"))
            .andExpect(jsonPath("$.selecionadas").value(1))
            .andExpect(jsonPath("$.faltantes").value(1));
    }

    @Test
    void confirmaEPersisteVinculoEStatus() throws Exception {
        Long id = requisicao(2);
        bolsa("B101", 1); bolsa("B102", 2);
        mvc.perform(post(url(id) + "/reserva").contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigosBolsas\":[\"B101\",\"B102\"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ALOCADA"));
        assertEquals(RESERVADA, bolsas.findById("B101").orElseThrow().getStatus());
        mvc.perform(get(url(id))).andExpect(jsonPath("$.codigosBolsas.length()").value(2));
        mvc.perform(get(url(id) + "/proposta-alocacao")).andExpect(status().isConflict());
        mvc.perform(post(url(id) + "/reserva").contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigosBolsas\":[\"B101\",\"B102\"]}"))
            .andExpect(status().isConflict());
    }

    @Test
    void revalidaPropostaAntigaSemReservarParcialmente() throws Exception {
        Long primeira = requisicao(2), segunda = requisicao(1);
        bolsa("B1", 1); bolsa("B2", 2);
        assertEquals(2, service.propor(primeira).selecionadas());
        service.confirmar(segunda, new ConfirmacaoReserva(List.of("B2")));
        mvc.perform(post(url(primeira) + "/reserva").contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigosBolsas\":[\"B1\",\"B2\"]}"))
            .andExpect(status().isConflict());
        assertEquals(DISPONIVEL, bolsas.findById("B1").orElseThrow().getStatus());
        assertEquals(StatusRequisicao.PENDENTE_ALOCACAO, service.consultar(primeira).status());
    }

    @Test
    void rejeitaQuantidadeIncorretaDuplicatasECamposInvalidos() throws Exception {
        Long id = requisicao(2);
        for (String json : List.of("{}", "{\"codigosBolsas\":[]}",
                "{\"codigosBolsas\":[\"B1\"]}",
                "{\"codigosBolsas\":[\"B1\",\"B1\"]}",
                "{\"codigosBolsas\":[\"B1\",\" \"]}")) {
            mvc.perform(post(url(id) + "/reserva").contentType(MediaType.APPLICATION_JSON)
                .content(json)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/requisicoes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"hospital\":\" \" ,\"quantidade\":0}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void retorna404ParaRequisicaoOuBolsaInexistente() throws Exception {
        mvc.perform(get(url(99999L) + "/proposta-alocacao")).andExpect(status().isNotFound());
        Long id = requisicao(1);
        mvc.perform(post(url(id) + "/reserva").contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigosBolsas\":[\"AUSENTE\"]}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejeitaBolsaIncompativelNaConfirmacao() throws Exception {
        Long id = requisicao(1);
        bolsa("B1", CONCENTRADO_HEMACIAS, B_POSITIVO, 1, DISPONIVEL);
        mvc.perform(post(url(id) + "/reserva").contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigosBolsas\":[\"B1\"]}"))
            .andExpect(status().isConflict());
        assertEquals(DISPONIVEL, bolsas.findById("B1").orElseThrow().getStatus());
    }

    @Test
    void revalidaVencimentoNaConfirmacao() throws Exception {
        Long id = requisicao(1); bolsa("B1", 0);
        assertEquals(1, service.propor(id).selecionadas());
        jdbc.update("update bolsa_hemo_componente set validade = ? where codigo = ?",
            LocalDate.now().minusDays(1), "B1");
        mvc.perform(post(url(id) + "/reserva").contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigosBolsas\":[\"B1\"]}"))
            .andExpect(status().isConflict());
    }

    @Test
    void criaRequisicaoPelaApiComLocation() throws Exception {
        mvc.perform(post("/api/v1/requisicoes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"hospital\":\"Aurora\",\"componente\":\"CONCENTRADO_HEMACIAS\","
                + "\"tipoSanguineo\":\"A_POSITIVO\",\"quantidade\":2}"))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("PENDENTE_ALOCACAO"));
    }

    @Test
    void duasRequisicoesConcorrentesNaoReservamAMesmaBolsa() throws Exception {
        Long r1 = requisicao(1), r2 = requisicao(1); bolsa("B1", 1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Future<Integer> primeira = executor.submit(() -> tentarReserva(r1, inicio));
            Future<Integer> segunda = executor.submit(() -> tentarReserva(r2, inicio));
            inicio.countDown();
            List<Integer> resultados = List.of(primeira.get(10, TimeUnit.SECONDS),
                segunda.get(10, TimeUnit.SECONDS)).stream().sorted().toList();
            assertEquals(List.of(200, 409), resultados);
            assertEquals(1, jdbc.queryForObject("select count(*) from requisicao_bolsa", Integer.class));
        } finally { executor.shutdownNow(); }
    }

    private int tentarReserva(Long id, CountDownLatch inicio) throws InterruptedException {
        inicio.await();
        try {
            service.confirmar(id, new ConfirmacaoReserva(List.of("B1")));
            return 200;
        } catch (ResponseStatusException erro) { return erro.getStatusCode().value(); }
    }
}
