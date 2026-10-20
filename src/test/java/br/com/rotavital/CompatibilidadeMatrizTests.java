package br.com.rotavital;

import br.com.rotavital.model.TipoComponente;
import br.com.rotavital.model.TipoSanguineo;
import br.com.rotavital.service.CompatibilidadeService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import static br.com.rotavital.model.TipoSanguineo.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Matriz acadêmica já adotada pelo projeto; não define regras clínicas novas. */
class CompatibilidadeMatrizTests {
    private final CompatibilidadeService service = new CompatibilidadeService();

    private static final Map<TipoSanguineo, Set<TipoSanguineo>> DOADORES_HEMACIAS = Map.of(
        O_NEGATIVO, Set.of(O_NEGATIVO),
        O_POSITIVO, Set.of(O_NEGATIVO, O_POSITIVO),
        A_NEGATIVO, Set.of(O_NEGATIVO, A_NEGATIVO),
        A_POSITIVO, Set.of(O_NEGATIVO, O_POSITIVO, A_NEGATIVO, A_POSITIVO),
        B_NEGATIVO, Set.of(O_NEGATIVO, B_NEGATIVO),
        B_POSITIVO, Set.of(O_NEGATIVO, O_POSITIVO, B_NEGATIVO, B_POSITIVO),
        AB_NEGATIVO, Set.of(O_NEGATIVO, A_NEGATIVO, B_NEGATIVO, AB_NEGATIVO),
        AB_POSITIVO, Set.of(TipoSanguineo.values())
    );

    static Stream<Arguments> matriz() {
        return Stream.of(TipoComponente.values()).flatMap(componente ->
            Stream.of(TipoSanguineo.values()).flatMap(receptor ->
                Stream.of(TipoSanguineo.values()).map(doador -> Arguments.of(
                    componente, doador, receptor,
                    componente == TipoComponente.CONCENTRADO_HEMACIAS
                        ? DOADORES_HEMACIAS.get(receptor).contains(doador)
                        : doador == receptor))));
    }

    @ParameterizedTest(name = "{0}: {1} -> {2} = {3}")
    @MethodSource("matriz")
    void preservaTodasAsCombinacoesDaRegraAtual(TipoComponente componente,
            TipoSanguineo doador, TipoSanguineo receptor, boolean esperado) {
        assertEquals(esperado, service.compativel(componente, doador, receptor));
    }
}
