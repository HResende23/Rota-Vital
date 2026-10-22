package br.com.rotavital;

import br.com.rotavital.model.User;
import br.com.rotavital.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RequisicaoRepository requisicoes;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpar() {
        requisicoes.deleteAll();
        users.deleteAll();
    }

    @Test
    void cadastraConsultaENormalizaEmailPersistido() throws Exception {
        mvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\" Ana \",\"email\":\"ANA@example.com\"}"))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.nome").value("Ana"))
            .andExpect(jsonPath("$.email").value("ana@example.com"));
        var user = users.findAll().get(0);
        mvc.perform(get("/api/v1/usuarios/" + user.getId()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Ana"));
        mvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Outra\",\"email\":\"ana@example.com\"}"))
            .andExpect(status().isConflict());
        assertEquals(1, users.count());
    }

    @Test
    void rejeitaDadosInvalidosEUsuarioAusente() throws Exception {
        for (String dados : java.util.List.of("{}", "{\"nome\":\" \",\"email\":\"a@example.com\"}",
                "{\"nome\":\"Ana\",\"email\":\"invalido\"}")) {
            mvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(dados)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/usuarios/" + Long.MAX_VALUE)).andExpect(status().isNotFound());
        assertEquals(0, users.count());
    }

    private String requisicao(String usuario) {
        return "{\"hospital\":\"Aurora\",\"componente\":\"PLASMA\","
            + "\"tipoSanguineo\":\"A_POSITIVO\",\"quantidade\":1" + usuario + "}";
    }

    @Test
    void exigeUsuarioExistenteSemPersistirRequisicaoInvalida() throws Exception {
        for (String usuario : java.util.List.of("", ",\"usuarioId\":null", ",\"usuarioId\":0")) {
            mvc.perform(post("/api/v1/requisicoes").contentType(MediaType.APPLICATION_JSON)
                .content(requisicao(usuario))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/requisicoes").contentType(MediaType.APPLICATION_JSON)
            .content(requisicao(",\"usuarioId\":" + Long.MAX_VALUE)))
            .andExpect(status().isNotFound());
        assertEquals(0, requisicoes.count());
    }

    @Test
    void persisteVariasRequisicoesDoMesmoUsuarioComChaveEstrangeira() throws Exception {
        Long id = users.saveAndFlush(new User("Ana", "ana@example.com")).getId();
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/requisicoes").contentType(MediaType.APPLICATION_JSON)
                .content(requisicao(",\"usuarioId\":" + id)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usuarioId").value(id));
        }
        assertEquals(2, jdbc.queryForObject(
            "select count(*) from requisicao where usuario_id = ?", Integer.class, id));
        Long requisicaoId = requisicoes.findAll().get(0).getId();
        mvc.perform(get("/api/v1/requisicoes/" + requisicaoId))
            .andExpect(status().isOk()).andExpect(jsonPath("$.usuarioId").value(id));
    }
}
