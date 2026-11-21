package br.com.rotavital;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.rotavital.dto.AnalisePearson;
import br.com.rotavital.dto.LeituraResposta;
import br.com.rotavital.model.Conectividade;
import br.com.rotavital.model.CondicaoLeitura;
import br.com.rotavital.service.EstatisticaService;

class EstatisticaServiceTests {

    private final EstatisticaService service = new EstatisticaService();

    @Test
    void deveCalcularCorrelacaoPositivaForte() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 21, 9, 0);

        List<LeituraResposta> leituras = List.of(
            new LeituraResposta(
                1L,
                inicio,
                4.0,
                Conectividade.CONECTADA,
                CondicaoLeitura.DENTRO_DA_FAIXA
            ),
            new LeituraResposta(
                2L,
                inicio.plusMinutes(1),
                5.5,
                Conectividade.CONECTADA,
                CondicaoLeitura.DENTRO_DA_FAIXA
            ),
            new LeituraResposta(
                3L,
                inicio.plusMinutes(2),
                7.0,
                Conectividade.CONECTADA,
                CondicaoLeitura.INICIO_DO_DESVIO
            )
        );

        AnalisePearson resultado = service.calcularPearson(leituras);

        assertNotNull(resultado);

        assertEquals(1.0, resultado.r(), 0.0001);
        assertEquals(1.0, resultado.r2(), 0.0001);

        assertEquals("positiva", resultado.direcao());
        assertEquals("muito forte", resultado.intensidade());
    }

    @Test
    void deveRetornarNullComApenasUmaLeitura() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 21, 9, 0);

        List<LeituraResposta> leituras = List.of(
            new LeituraResposta(
                1L,
                inicio,
                4.0,
                Conectividade.CONECTADA,
                CondicaoLeitura.DENTRO_DA_FAIXA
            )
        );

        AnalisePearson resultado = service.calcularPearson(leituras);

        assertNull(resultado);
    }
}   