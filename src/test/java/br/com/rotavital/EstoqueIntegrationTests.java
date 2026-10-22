package br.com.rotavital;

import br.com.rotavital.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.security.test.context.support.WithMockUser(username = "solicitante@example.com")
@AutoConfigureMockMvc
class EstoqueIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired BolsaHemoComponenteRepository bolsas;
    @Autowired RequisicaoRepository requisicoes;

    @BeforeEach
    void limpar() {
        requisicoes.deleteAll();
        bolsas.deleteAll();
    }

    private String payload(String codigo, String validade, String status) {
        return """
            {"codigo":"%s","componente":"CONCENTRADO_HEMACIAS",
             "tipoSanguineo":"A_POSITIVO","validade":"%s","status":"%s"}
            """.formatted(codigo, validade, status);
    }

    @Test
    void listaEstoqueVazio() throws Exception {
        mvc.perform(get("/api/v1/bolsas")).andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void cadastraPersisteEConsultaBolsaValidaHoje() throws Exception {
        mvc.perform(post("/api/v1/bolsas").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(payload("B1", LocalDate.now().toString(), "DISPONIVEL")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.codigo").value("B1"));
        assertEquals(1, bolsas.count());
        mvc.perform(get("/api/v1/bolsas")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tipoSanguineo").value("A_POSITIVO"));
    }

    @Test
    void duplicataNaoSobrescreveDadosOriginais() throws Exception {
        mvc.perform(post("/api/v1/bolsas").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(payload("B1", LocalDate.now().toString(), "DISPONIVEL")))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/bolsas").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(payload("B1", LocalDate.now().plusDays(3).toString(), "RESERVADA")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").exists());
        var bolsa = bolsas.findById("B1").orElseThrow();
        assertEquals(LocalDate.now(), bolsa.getValidade());
        assertEquals("DISPONIVEL", bolsa.getStatus().name());
        assertEquals(1, bolsas.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"componente\":\"INVALIDO\"}",
        "{\"validade\":\"data-invalida\"}", "null"})
    void rejeitaCorpoInvalidoSemPersistir(String json) throws Exception {
        mvc.perform(post("/api/v1/bolsas").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json)).andExpect(status().isBadRequest());
        assertEquals(0, bolsas.count());
    }

    @Test
    void rejeitaBolsaVencida() throws Exception {
        mvc.perform(post("/api/v1/bolsas").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(payload("B1", LocalDate.now().minusDays(1).toString(), "DISPONIVEL")))
            .andExpect(status().isBadRequest());
        assertEquals(0, bolsas.count());
    }

}
