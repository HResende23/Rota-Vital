package br.com.rotavital;

import br.com.rotavital.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.rotavital.model.TipoComponente.CONCENTRADO_HEMACIAS;
import static br.com.rotavital.model.TipoSanguineo.A_POSITIVO;

class DominioTests {
    private BolsaHemoComponente bolsa(String codigo, int dias, StatusBolsa status) {
        return new BolsaHemoComponente(codigo, CONCENTRADO_HEMACIAS, A_POSITIVO,
            LocalDate.now().plusDays(dias), status);
    }

    @Test
    void bolsaValidaHojePodeSerReservadaUmaUnicaVez() {
        var bolsa = bolsa("B1", 0, StatusBolsa.DISPONIVEL);
        assertFalse(bolsa.estaVencida());
        assertTrue(bolsa.estaDisponivel());
        bolsa.reservar();
        assertEquals(StatusBolsa.RESERVADA, bolsa.getStatus());
        assertThrows(IllegalStateException.class, bolsa::reservar);
    }

    @ParameterizedTest
    @EnumSource(StatusBolsa.class)
    void bolsaVencidaNuncaPodeSerReservada(StatusBolsa status) {
        var bolsa = bolsa("B1", -1, status);
        assertTrue(bolsa.estaVencida());
        assertFalse(bolsa.estaDisponivel());
        assertThrows(IllegalStateException.class, bolsa::reservar);
        assertEquals(status, bolsa.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = StatusBolsa.class, names = {"RESERVADA", "INDISPONIVEL"})
    void statusBloqueiaReservaMesmoDentroDaValidade(StatusBolsa status) {
        var bolsa = bolsa("B1", 1, status);
        assertFalse(bolsa.estaDisponivel());
        assertThrows(IllegalStateException.class, bolsa::reservar);
        assertEquals(status, bolsa.getStatus());
    }

    @Test
    void alocacaoPreservaVinculosEImpedeNovaAlocacao() {
        var requisicao = new Requisicao(new User("Ana", "ana@example.com"), "Hospital", CONCENTRADO_HEMACIAS, A_POSITIVO, 1);
        var bolsa = bolsa("B1", 1, StatusBolsa.RESERVADA);
        requisicao.alocar(List.of(bolsa));
        assertEquals(StatusRequisicao.ALOCADA, requisicao.getStatus());
        assertEquals(List.of(bolsa), requisicao.getBolsas());
        assertThrows(UnsupportedOperationException.class, () -> requisicao.getBolsas().clear());
        assertThrows(IllegalStateException.class, () -> requisicao.alocar(List.of(bolsa)));
    }

    @Test
    void rejeitaQuantidadeInsuficienteSemAlterarRequisicao() {
        var requisicao = new Requisicao(new User("Ana", "ana@example.com"), "Hospital", CONCENTRADO_HEMACIAS, A_POSITIVO, 2);
        assertThrows(IllegalStateException.class,
            () -> requisicao.alocar(List.of(bolsa("B1", 1, StatusBolsa.RESERVADA))));
        assertEquals(StatusRequisicao.PENDENTE_ALOCACAO, requisicao.getStatus());
        assertTrue(requisicao.getBolsas().isEmpty());
    }

}
