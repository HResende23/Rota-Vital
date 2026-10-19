package br.com.rotavital;

import br.com.rotavital.model.*;
import br.com.rotavital.service.CompatibilidadeService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompatibilidadeServiceTests {
    private final CompatibilidadeService service = new CompatibilidadeService();

    @ParameterizedTest
    @CsvSource({
        "CONCENTRADO_HEMACIAS,O_NEGATIVO,A_POSITIVO,true",
        "CONCENTRADO_HEMACIAS,O_POSITIVO,A_NEGATIVO,false",
        "CONCENTRADO_HEMACIAS,B_POSITIVO,A_POSITIVO,false",
        "CONCENTRADO_HEMACIAS,A_NEGATIVO,AB_POSITIVO,true",
        "CONCENTRADO_HEMACIAS,AB_POSITIVO,O_POSITIVO,false",
        "CONCENTRADO_HEMACIAS,A_POSITIVO,A_POSITIVO,true",
        "PLASMA,O_NEGATIVO,A_POSITIVO,false",
        "PLASMA,A_POSITIVO,A_POSITIVO,true",
        "PLAQUETAS,O_NEGATIVO,A_POSITIVO,false",
        "PLAQUETAS,A_POSITIVO,A_POSITIVO,true"
    })
    void aplicaRegraDidaticaPorComponente(TipoComponente componente, TipoSanguineo doador,
                                         TipoSanguineo receptor, boolean esperado) {
        assertEquals(esperado, service.compativel(componente, doador, receptor));
    }
}
