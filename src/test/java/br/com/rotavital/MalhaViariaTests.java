package br.com.rotavital;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import br.com.rotavital.model.Caminho;
import br.com.rotavital.model.HorarioDePico;
import br.com.rotavital.model.PontoDaMalha;
import br.com.rotavital.service.MalhaViaria;
import static br.com.rotavital.service.MalhaViaria.HEMOCENTRO;
import static org.junit.jupiter.api.Assertions.*;

class MalhaViariaTests {
    private final MalhaViaria malha = new MalhaViaria();

    private Map<String, Caminho> caminhosAte(String hospital) {
        return malha.caminhos(HEMOCENTRO, hospital, 4).stream()
            .collect(Collectors.toMap(Caminho::getChave, c -> c));
    }

    @Test
    void comparaCaminhosAteOHospitalAurora() {
        var caminhos = caminhosAte("AURORA");
        assertEquals(40, caminhos.get("HEMOCENTRO>AURORA").duracao(false));
        assertEquals(44, caminhos.get("HEMOCENTRO>P3>AURORA").duracao(false));
        assertEquals(50, caminhos.get("HEMOCENTRO>P1>AURORA").duracao(false));
        assertEquals("Hemocentro Central → Anel Viário (P1) → Hospital Aurora",
            caminhos.get("HEMOCENTRO>P1>AURORA").getDescricao());
    }

    @Test
    void picoDeixaViasUrbanasMaisLentasEMudaOMelhorCaminho() {
        var caminhos = caminhosAte("AURORA");
        assertEquals(60, caminhos.get("HEMOCENTRO>AURORA").duracao(true));
        assertEquals(66, caminhos.get("HEMOCENTRO>P3>AURORA").duracao(true));
        assertEquals(50, caminhos.get("HEMOCENTRO>P1>AURORA").duracao(true), "vias expressas não mudam");
    }

    @Test
    void cadaHospitalTemAlternativasSemPassarPorOutroHospital() {
        for (PontoDaMalha hospital : malha.hospitais()) {
            var caminhos = malha.caminhos(HEMOCENTRO, hospital.codigo(), 4);
            assertTrue(caminhos.size() >= 2, hospital.nome());
            for (Caminho caminho : caminhos) {
                var pontos = caminho.pontos();
                assertEquals(HEMOCENTRO, pontos.get(0).codigo());
                assertEquals(hospital, pontos.get(pontos.size() - 1));
                assertTrue(pontos.subList(1, pontos.size() - 1).stream().noneMatch(PontoDaMalha::isHospital));
                assertEquals(pontos.size(), new HashSet<>(pontos).size(), "sem pontos repetidos");
                assertTrue(caminho.trechos().size() <= 4);
            }
        }
    }

    @Test
    void limiteDeTrechosRestringeAsAlternativas() {
        assertEquals(1, malha.caminhos(HEMOCENTRO, "AURORA", 1).size());
        assertTrue(malha.caminhos(HEMOCENTRO, "VALE_VERDE", 1).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> malha.caminhos(HEMOCENTRO, "INEXISTENTE", 4));
    }

    @ParameterizedTest
    @CsvSource({"Hospital Aurora,AURORA", "aurora,AURORA", "'  HOSPITAL   aurora ',AURORA",
        "Hospitál Áurora,AURORA", "Hospital Vale Verde,VALE_VERDE", "bom pastor,BOM_PASTOR"})
    void reconheceOHospitalDigitadoNaRequisicao(String digitado, String codigo) {
        assertEquals(codigo, malha.hospitalCorrespondente(digitado).orElseThrow().codigo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Hospital Web", "Outro hospital", "Hemocentro Central", " "})
    void naoInventaCorrespondenciaParaHospitalForaDaMalha(String digitado) {
        assertTrue(malha.hospitalCorrespondente(digitado).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"06:59,false", "07:00,true", "08:59,true", "09:00,false", "12:00,false",
        "17:00,true", "18:59,true", "19:00,false"})
    void horarioDePicoConsideraManhaETarde(LocalTime horario, boolean esperado) {
        assertEquals(esperado, HorarioDePico.em(horario));
    }
}
