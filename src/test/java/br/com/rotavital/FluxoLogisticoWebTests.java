package br.com.rotavital;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
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

/** Telas de rota e monitoramento renderizadas com dados reais, da reserva até a entrega. */
@SpringBootTest
@AutoConfigureMockMvc
class FluxoLogisticoWebTests {
    private static final String AMANHA_0930 = LocalDate.now().plusDays(1) + "T09:30";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RequisicaoRepository requisicoes;
    @Autowired BolsaHemoComponenteRepository bolsas;
    @Autowired RemessaRepository remessas;
    @Autowired AlocacaoService alocacao;
    @Autowired MonitoramentoService monitoramento;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void limpar() {
        remessas.deleteAll();
        requisicoes.deleteAll();
        bolsas.deleteAll();
        users.deleteAll();
    }

    private MockHttpSession entrar(String email) throws Exception {
        users.saveAndFlush(new User("Ana", email, encoder.encode("senhaTeste123")));
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("email", email).param("senha", "senhaTeste123"))
            .andExpect(redirectedUrl("/estoque")).andReturn().getRequest().getSession(false);
    }

    /** Cria bolsa e requisição de 1 bolsa e confirma a reserva pela interface. Devolve o id da remessa. */
    private Long reservarPelaTela(MockHttpSession sessao, String hospital) throws Exception {
        mvc.perform(post("/bolsas").session(sessao).with(csrf()).param("codigo", "WEB-CH-1")
                .param("componente", "CONCENTRADO_HEMACIAS").param("tipoSanguineo", "A_POSITIVO")
                .param("validade", LocalDate.now().plusDays(5).toString()))
            .andExpect(redirectedUrl("/estoque"));
        mvc.perform(post("/requisicoes").session(sessao).with(csrf()).param("hospital", hospital)
                .param("componente", "CONCENTRADO_HEMACIAS").param("tipoSanguineo", "A_POSITIVO")
                .param("quantidade", "1"))
            .andExpect(status().is3xxRedirection());
        Long requisicaoId = requisicoes.findAll().get(0).getId();
        mvc.perform(post("/alocacao/" + requisicaoId + "/reserva").session(sessao).with(csrf())
                .param("codigosBolsas", "WEB-CH-1"))
            .andExpect(redirectedUrl("/alocacao?requisicaoId=" + requisicaoId))
            .andExpect(flash().attribute("sucesso", containsString("monitoramento térmico da remessa começou")));
        mvc.perform(get("/alocacao").session(sessao).param("requisicaoId", requisicaoId.toString()))
            .andExpect(content().string(containsString("Requisição alocada")))
            .andExpect(content().string(containsString("Acompanhar monitoramento")));
        return remessas.findByRequisicaoId(requisicaoId).orElseThrow().getId();
    }

    private void planejar(MockHttpSession sessao, Long remessaId) throws Exception {
        mvc.perform(post("/rota/" + remessaId + "/planejamento").session(sessao).with(csrf())
                .param("destino", "AURORA").param("saida", AMANHA_0930)
                .param("janelaInicio", "10:00").param("janelaFim", "11:00"))
            .andExpect(redirectedUrl("/monitoramento?remessaId=" + remessaId))
            .andExpect(flash().attribute("sucesso", containsString("Hemocentro Central → Hospital Aurora")));
    }

    private String tela(MockHttpSession sessao, String caminho) throws Exception {
        return mvc.perform(get(caminho).session(sessao)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    @Test
    void reservaAbreRemessaQueEhPlanejadaEMonitoradaAteOAlerta() throws Exception {
        var sessao = entrar("ana@example.com");
        Long remessaId = reservarPelaTela(sessao, "hospital aurora");

        String monitoramento = tela(sessao, "/monitoramento");
        assertTrue(monitoramento.contains("Aguardando rota"));
        assertTrue(monitoramento.contains("Aguardando a primeira leitura"));
        assertTrue(monitoramento.contains("Planejar rota"));

        String rota = tela(sessao, "/rota");
        assertTrue(rota.contains("Rota viável"), "a sugestão padrão já deve ser viável");
        assertTrue(rota.contains("Alternativa proposta:"));
        assertTrue(rota.contains("Confirmar planejamento"));
        assertTrue(rota.contains("<option value=\"AURORA\" selected=\"selected\">Hospital Aurora</option>"),
            "o hospital digitado na requisição é reconhecido na malha");

        planejar(sessao, remessaId);
        String planejada = tela(sessao, "/monitoramento?remessaId=" + remessaId);
        assertTrue(planejada.contains("Rota planejada"));
        assertTrue(planejada.contains("Iniciar transporte"));

        mvc.perform(post("/monitoramento/" + remessaId + "/leituras").session(sessao).with(csrf())
                .param("temperatura", "7.0").param("conectividade", "CONECTADA"))
            .andExpect(redirectedUrl("/monitoramento?remessaId=" + remessaId))
            .andExpect(flash().attribute("sucesso", containsString("desvio iniciado")));
        assertTrue(tela(sessao, "/monitoramento?remessaId=" + remessaId).contains("Temperatura fora da faixa há"));

        mvc.perform(post("/monitoramento/" + remessaId + "/leituras").session(sessao).with(csrf())
                .param("temperatura", "8.0").param("conectividade", "INSTAVEL"))
            .andExpect(flash().attribute("sucesso", containsString("Alerta térmico")));
        String alerta = tela(sessao, "/monitoramento?remessaId=" + remessaId);
        assertTrue(alerta.contains("Reconhecer alerta"));
        assertTrue(alerta.contains("WEB-CH-1 bloqueada no estoque"));
        assertTrue(alerta.contains("Sinal instável"));

        mvc.perform(post("/monitoramento/" + remessaId + "/alerta/reconhecimento").session(sessao).with(csrf()))
            .andExpect(flash().attribute("sucesso", containsString("continuam bloqueadas")));
        assertTrue(tela(sessao, "/monitoramento?remessaId=" + remessaId)
            .contains("Alerta reconhecido por ana@example.com"));
        assertTrue(tela(sessao, "/estoque").contains("Indisponível"));

        mvc.perform(post("/monitoramento/" + remessaId + "/transporte").session(sessao).with(csrf()))
            .andExpect(flash().attribute("erro", containsString("alerta térmico")));
        String rotaBloqueada = tela(sessao, "/rota?remessaId=" + remessaId);
        assertTrue(rotaBloqueada.contains("alerta térmico pendente de avaliação"));
        assertFalse(rotaBloqueada.contains("Confirmar planejamento"));
    }

    @Test
    void entregaPelaTelaEncerraOMonitoramento() throws Exception {
        var sessao = entrar("ana@example.com");
        Long remessaId = reservarPelaTela(sessao, "Hospital Aurora");
        planejar(sessao, remessaId);
        mvc.perform(post("/monitoramento/" + remessaId + "/transporte").session(sessao).with(csrf()))
            .andExpect(flash().attribute("sucesso", containsString("saiu do hemocentro")));
        String transporte = tela(sessao, "/monitoramento?remessaId=" + remessaId);
        assertTrue(transporte.contains("Confirmar entrega"));
        assertTrue(transporte.contains("Em transporte desde"));

        mvc.perform(post("/monitoramento/" + remessaId + "/entrega").session(sessao).with(csrf()))
            .andExpect(flash().attribute("sucesso", containsString("monitoramento foi encerrado")));
        String entregue = tela(sessao, "/monitoramento?remessaId=" + remessaId);
        assertTrue(entregue.contains("monitoramento encerrado"));
        assertFalse(entregue.contains("Registrar leitura"));
        assertTrue(tela(sessao, "/rota?remessaId=" + remessaId).contains("já foi entregue"));
    }

    @Test
    void hospitalForaDaMalhaEParametrosInvalidosSaoExplicados() throws Exception {
        var sessao = entrar("ana@example.com");
        Long remessaId = reservarPelaTela(sessao, "Hospital Web");
        String rota = tela(sessao, "/rota");
        assertTrue(rota.contains("não foi reconhecido na malha"));
        assertFalse(rota.contains("Confirmar planejamento"));

        String janelaInvertida = tela(sessao, "/rota?remessaId=" + remessaId + "&destino=AURORA&saida="
            + AMANHA_0930 + "&janelaInicio=11:00&janelaFim=10:00");
        assertTrue(janelaInvertida.contains("A janela de recebimento deve terminar depois de começar"));

        mvc.perform(post("/rota/" + remessaId + "/planejamento").session(sessao).with(csrf())
                .param("destino", "AURORA").param("janelaInicio", "10:00").param("janelaFim", "11:00"))
            .andExpect(redirectedUrl("/rota?remessaId=" + remessaId))
            .andExpect(flash().attribute("erro", containsString("Informe o hospital de destino")));
        mvc.perform(post("/rota/" + remessaId + "/planejamento").session(sessao).with(csrf())
                .param("destino", "AURORA").param("saida", LocalDate.now().plusDays(1) + "T10:40")
                .param("janelaInicio", "10:00").param("janelaFim", "11:00"))
            .andExpect(header().string("Location", startsWith("/rota?remessaId=" + remessaId + "&destino=AURORA")))
            .andExpect(flash().attribute("erro", containsString("Nenhum caminho atende")));
        assertEquals(StatusRemessa.AGUARDANDO_ROTA, remessas.findById(remessaId).orElseThrow().getStatus());
    }

    @Test
    void leituraInvalidaNaTelaNaoERegistrada() throws Exception {
        var sessao = entrar("ana@example.com");
        Long remessaId = reservarPelaTela(sessao, "Hospital Aurora");
        for (String temperatura : List.of("quente", "99", "")) {
            mvc.perform(post("/monitoramento/" + remessaId + "/leituras").session(sessao).with(csrf())
                    .param("temperatura", temperatura))
                .andExpect(redirectedUrl("/monitoramento?remessaId=" + remessaId))
                .andExpect(flash().attribute("erro", containsString("Informe uma temperatura")));
        }
        assertTrue(monitoramento.consultar(remessaId).leituras().isEmpty());
    }

    @Test
    void reservaAnteriorAoMonitoramentoPodeAbrirRemessa() throws Exception {
        var sessao = entrar("ana@example.com");
        Long remessaId = reservarPelaTela(sessao, "Hospital Aurora");
        Long requisicaoId = requisicoes.findAll().get(0).getId();
        remessas.deleteById(remessaId); // simula uma reserva feita antes desta entrega
        String alocacaoSemRemessa = tela(sessao, "/alocacao?requisicaoId=" + requisicaoId);
        assertTrue(alocacaoSemRemessa.contains("Iniciar monitoramento"));
        assertFalse(alocacaoSemRemessa.contains("Acompanhar monitoramento"));

        mvc.perform(post("/alocacao/" + requisicaoId + "/monitoramento").session(sessao).with(csrf()))
            .andExpect(redirectedUrl("/alocacao?requisicaoId=" + requisicaoId))
            .andExpect(flash().attribute("sucesso", containsString("Monitoramento iniciado")));
        assertTrue(remessas.findByRequisicaoId(requisicaoId).isPresent());
        mvc.perform(post("/alocacao/" + requisicaoId + "/monitoramento").session(sessao).with(csrf()))
            .andExpect(flash().attribute("erro", containsString("já foi iniciado")));
        assertEquals(1, remessas.count());
    }

    @Test
    void telasNaoExpoemRemessaDeOutroUsuario() throws Exception {
        var sessao = entrar("ana@example.com");
        var outro = users.saveAndFlush(new User("Outro", "outro@example.com", encoder.encode("senhaTeste123")));
        bolsas.saveAndFlush(new BolsaHemoComponente("B-OUTRO", TipoComponente.PLASMA, TipoSanguineo.O_POSITIVO,
            LocalDate.now().plusDays(3), StatusBolsa.DISPONIVEL));
        Long requisicao = alocacao.cadastrar(new NovaRequisicao(outro.getId(), "Hospital Primavera",
            TipoComponente.PLASMA, TipoSanguineo.O_POSITIVO, 1)).id();
        alocacao.confirmar(requisicao, new ConfirmacaoReserva(List.of("B-OUTRO")));
        Long remessaId = remessas.findByRequisicaoId(requisicao).orElseThrow().getId();

        assertTrue(tela(sessao, "/monitoramento").contains("Nenhuma remessa monitorada"));
        assertTrue(tela(sessao, "/rota").contains("Nenhuma remessa para planejar"));
        mvc.perform(get("/monitoramento").session(sessao).param("remessaId", remessaId.toString()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/rota").session(sessao).param("remessaId", remessaId.toString()))
            .andExpect(status().isForbidden());
        mvc.perform(post("/monitoramento/" + remessaId + "/leituras").session(sessao).with(csrf())
            .param("temperatura", "-20")).andExpect(status().isForbidden());
        mvc.perform(post("/rota/" + remessaId + "/planejamento").session(sessao).with(csrf())
            .param("destino", "PRIMAVERA").param("saida", AMANHA_0930)
            .param("janelaInicio", "10:00").param("janelaFim", "11:00")).andExpect(status().isForbidden());
        mvc.perform(post("/alocacao/" + requisicao + "/monitoramento").session(sessao).with(csrf()))
            .andExpect(status().isForbidden());
        assertTrue(monitoramento.consultar(remessaId).leituras().isEmpty());
    }
}
