package br.com.rotavital;

import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import br.com.rotavital.model.*;
import br.com.rotavital.repository.*;
import br.com.rotavital.dto.*;
import br.com.rotavital.service.AlocacaoService;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FluxoWebIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RequisicaoRepository requisicoes;
    @Autowired BolsaHemoComponenteRepository bolsas;
    @Autowired PasswordEncoder encoder;
    @Autowired AlocacaoService alocacao;

    @BeforeEach
    void limpar() {
        requisicoes.deleteAll();
        bolsas.deleteAll();
        users.deleteAll();
    }

    @Test
    void paginasPublicasEProtegidasNaoExibemDadosSemLogin() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Criar conta")));
        mvc.perform(get("/registro")).andExpect(status().isOk());
        for (String path : java.util.List.of("/estoque", "/requisicao", "/alocacao",
                "/rota", "/monitoramento", "/index.html")) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
        }
        mvc.perform(get("/api/v1/bolsas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/registro").param("nome", "Ana")).andExpect(status().isForbidden());
    }

    private MockHttpSession registrarEEntrar() throws Exception {
        mvc.perform(post("/registro").with(csrf()).param("nome", "Ana")
            .param("email", "ANA@example.com").param("senha", "senhaTeste123")
            .param("confirmacao", "senhaTeste123"))
            .andExpect(redirectedUrl("/login?cadastrado"));
        var user = users.findByEmail("ana@example.com").orElseThrow();
        assertNotEquals("senhaTeste123", user.getSenhaHash());
        assertTrue(encoder.matches("senhaTeste123", user.getSenhaHash()));
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
            .param("email", "ANA@example.com").param("senha", "senhaTeste123"))
            .andExpect(redirectedUrl("/estoque")).andReturn().getRequest().getSession(false);
    }

    @Test
    void fluxoCompletoRegistroLoginRequisicaoReservaLogout() throws Exception {
        MockHttpSession session = registrarEEntrar();
        mvc.perform(get("/estoque").session(session)).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhuma bolsa encontrada")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Sair")));
        mvc.perform(get("/requisicao").session(session)).andExpect(status().isOk());
        mvc.perform(post("/bolsas").session(session).with(csrf()).param("codigo", "WEB-1")
            .param("componente", "PLASMA").param("tipoSanguineo", "A_POSITIVO")
            .param("validade", LocalDate.now().plusDays(5).toString()))
            .andExpect(redirectedUrl("/estoque"));
        mvc.perform(get("/estoque").session(session).param("componente", "PLASMA"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("WEB-1")));
        mvc.perform(get("/estoque").session(session).param("componente", "PLAQUETAS"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhuma bolsa encontrada")));
        mvc.perform(post("/requisicoes").session(session).with(csrf())
            .param("hospital", "Hospital Web").param("componente", "PLASMA")
            .param("tipoSanguineo", "A_POSITIVO").param("quantidade", "1")
            .param("usuarioId", "99999"))
            .andExpect(status().is3xxRedirection());
        var requisicao = requisicoes.findAll().get(0);
        Long id = requisicao.getId();
        assertEquals(users.findByEmail("ana@example.com").orElseThrow().getId(),
            mvcUsuarioId(id));
        mvc.perform(get("/alocacao").session(session).param("requisicaoId", id.toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("WEB-1")));
        mvc.perform(post("/alocacao/" + id + "/reserva").session(session).with(csrf())
            .param("codigosBolsas", "WEB-1"))
            .andExpect(redirectedUrl("/alocacao?requisicaoId=" + id));
        assertEquals(StatusBolsa.RESERVADA, bolsas.findById("WEB-1").orElseThrow().getStatus());
        mvc.perform(get("/alocacao").session(session).param("requisicaoId", id.toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Requisição alocada")));
        mvc.perform(post("/logout").session(session).with(csrf()))
            .andExpect(redirectedUrl("/login?saiu"));
        assertTrue(session.isInvalid());
    }

    private Long mvcUsuarioId(Long id) { return alocacao.consultar(id).usuarioId(); }

    @Test
    void rejeitaCredenciaisIncorretasDuplicataEConfirmacaoDeSenha() throws Exception {
        registrarEEntrar();
        mvc.perform(post("/login").with(csrf()).param("email", "ana@example.com").param("senha", "errada"))
            .andExpect(redirectedUrl("/login?erro"));
        mvc.perform(post("/registro").with(csrf()).param("nome", "Outra")
            .param("email", "ana@example.com").param("senha", "senhaTeste123")
            .param("confirmacao", "senhaTeste123"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("E-mail já cadastrado")));
        mvc.perform(post("/registro").with(csrf()).param("nome", "Outra")
            .param("email", "outra@example.com").param("senha", "senhaTeste123")
            .param("confirmacao", "diferente"))
            .andExpect(status().isOk());
        assertEquals(1, users.count());
        mvc.perform(post("/registro").with(csrf()).param("nome", "Outra")
            .param("email", "outra@example.com").param("senha", "curta").param("confirmacao", "curta"))
            .andExpect(status().isOk());
        assertEquals(1, users.count());
    }

    @Test
    void bloqueiaAcessoEAlteracaoDeRequisicaoDeOutroUsuario() throws Exception {
        var session = registrarEEntrar();
        var outro = users.saveAndFlush(new User("Outro", "outro@example.com", encoder.encode("senhaTeste123")));
        Long id = alocacao.cadastrar(new NovaRequisicao(outro.getId(), "Outro hospital",
            TipoComponente.PLASMA, TipoSanguineo.A_POSITIVO, 1)).id();
        mvc.perform(get("/alocacao").session(session).param("requisicaoId", id.toString()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/requisicoes/" + id).session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/alocacao/" + id + "/reserva").session(session).with(csrf())
            .param("codigosBolsas", "B1")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/requisicoes").session(session).with(csrf())
            .contentType("application/json").content("{\"usuarioId\":" + outro.getId()
                + ",\"hospital\":\"H\",\"componente\":\"PLASMA\",\"tipoSanguineo\":\"A_POSITIVO\",\"quantidade\":1}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/requisicoes").session(session).param("hospital", "H"))
            .andExpect(status().isForbidden());
    }

    @Test
    void cadastroLegadoSemSenhaNaoAutentica() throws Exception {
        users.saveAndFlush(new User("Legado", "legado@example.com"));
        mvc.perform(post("/login").with(csrf()).param("email", "legado@example.com").param("senha", "qualquer"))
            .andExpect(redirectedUrl("/login?erro"));
    }
}
