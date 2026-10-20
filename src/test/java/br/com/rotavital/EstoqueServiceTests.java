package br.com.rotavital;

import br.com.rotavital.model.*;
import br.com.rotavital.repository.BolsaHemoComponenteRepository;
import br.com.rotavital.service.EstoqueService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstoqueServiceTests {
    private final BolsaHemoComponenteRepository repository = mock(BolsaHemoComponenteRepository.class);
    private final EstoqueService service = new EstoqueService(repository);

    private BolsaHemoComponente bolsa() {
        return new BolsaHemoComponente("B1", TipoComponente.PLASMA, TipoSanguineo.O_NEGATIVO,
            LocalDate.now().plusDays(1), StatusBolsa.DISPONIVEL);
    }

    @Test
    void duplicataNaoChegaAOperacaoDeGravacao() {
        when(repository.existsById("B1")).thenReturn(true);
        var erro = assertThrows(IllegalArgumentException.class, () -> service.cadastrar(bolsa()));
        assertEquals("Já existe uma bolsa cadastrada com o código B1", erro.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void propagaFalhaDePersistenciaSemSimularSucesso() {
        var bolsa = bolsa();
        var falha = new IllegalStateException("Banco indisponível");
        when(repository.save(bolsa)).thenThrow(falha);
        assertSame(falha, assertThrows(IllegalStateException.class, () -> service.cadastrar(bolsa)));
    }
}
