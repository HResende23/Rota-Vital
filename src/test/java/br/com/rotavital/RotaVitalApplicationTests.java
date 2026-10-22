package br.com.rotavital;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.security.test.context.support.WithMockUser(username = "solicitante@example.com")
@AutoConfigureMockMvc
class RotaVitalApplicationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired br.com.rotavital.repository.UserRepository users;

    @org.junit.jupiter.api.BeforeEach
    void usuario() {
        if (!users.existsByEmail("solicitante@example.com")) {
            users.saveAndFlush(new br.com.rotavital.model.User("Solicitante", "solicitante@example.com"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/index.html", "/requisicao.html", "/alocacao.html",
        "/rota.html", "/monitoramento.html"
    })
    void shouldServeWireframes(String path) throws Exception {
        mockMvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
