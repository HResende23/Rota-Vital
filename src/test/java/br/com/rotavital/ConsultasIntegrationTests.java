package br.com.rotavital;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManagerFactory;
import br.com.rotavital.dto.*;
import br.com.rotavital.model.*;
import br.com.rotavital.repository.*;
import br.com.rotavital.service.AlocacaoService;
import br.com.rotavital.service.MonitoramentoService;
import static org.junit.jupiter.api.Assertions.*;

/**
 * As listagens das telas trazem as associações que exibem numa única consulta (sem N+1),
 * e as chaves estrangeiras consultadas com frequência têm índice — o PostgreSQL não cria
 * índice para chave estrangeira por conta própria.
 */
@SpringBootTest
class ConsultasIntegrationTests {
    private static final int RESERVAS = 3;

    @Autowired AlocacaoService alocacao;
    @Autowired MonitoramentoService monitoramento;
    @Autowired RemessaRepository remessas;
    @Autowired RequisicaoRepository requisicoes;
    @Autowired BolsaHemoComponenteRepository bolsas;
    @Autowired UserRepository users;
    @Autowired EntityManagerFactory fabrica;
    @Autowired DataSource dataSource;
    private Long usuarioId;

    /** Três requisições de uma bolsa cada, todas reservadas (e, portanto, com remessa aberta). */
    @BeforeEach
    void prepararReservas() {
        remessas.deleteAll();
        requisicoes.deleteAll();
        bolsas.deleteAll();
        users.deleteAll();
        usuarioId = users.saveAndFlush(new User("Solicitante", "consultas@example.com")).getId();
        for (int i = 1; i <= RESERVAS; i++) {
            String codigo = "C" + i;
            bolsas.saveAndFlush(new BolsaHemoComponente(codigo, TipoComponente.CONCENTRADO_HEMACIAS,
                TipoSanguineo.O_NEGATIVO, LocalDate.now().plusDays(5), StatusBolsa.DISPONIVEL));
            Long requisicao = alocacao.cadastrar(new NovaRequisicao(usuarioId, "Hospital Aurora",
                TipoComponente.CONCENTRADO_HEMACIAS, TipoSanguineo.A_POSITIVO, 1)).id();
            alocacao.confirmar(requisicao, new ConfirmacaoReserva(List.of(codigo)));
        }
    }

    private record Medicao<T>(T resultado, long comandosSql) {}

    /** Executa a consulta com as estatísticas do Hibernate ligadas e conta os comandos SQL preparados. */
    private <T> Medicao<T> medir(Supplier<T> consulta) {
        Statistics estatisticas = fabrica.unwrap(SessionFactory.class).getStatistics();
        estatisticas.clear();
        estatisticas.setStatisticsEnabled(true);
        try {
            T resultado = consulta.get();
            return new Medicao<>(resultado, estatisticas.getPrepareStatementCount());
        } finally {
            estatisticas.setStatisticsEnabled(false);
        }
    }

    @Test
    void listagemDeRemessasTrazAsRequisicoesNaMesmaConsulta() {
        Medicao<List<RemessaResumo>> medicao = medir(() -> monitoramento.listarDoUsuario(usuarioId));

        assertEquals(RESERVAS, medicao.resultado().size());
        assertTrue(medicao.resultado().stream()
            .allMatch(r -> r.hospital().equals("Hospital Aurora") && r.quantidadeBolsas() == 1));
        assertEquals(1, medicao.comandosSql(),
            "a listagem não pode buscar a requisição de cada remessa numa consulta separada");
    }

    @Test
    void listagemDeRequisicoesTrazAsBolsasNaMesmaConsulta() {
        Medicao<List<RequisicaoResposta>> medicao = medir(() -> alocacao.listarDoUsuario(usuarioId));

        assertEquals(List.of(List.of("C3"), List.of("C2"), List.of("C1")),
            medicao.resultado().stream().map(RequisicaoResposta::codigosBolsas).toList(),
            "mais recentes primeiro, cada uma com a sua bolsa e sem linhas repetidas");
        assertEquals(1, medicao.comandosSql(),
            "a listagem não pode buscar as bolsas de cada requisição numa consulta separada");
    }

    @Test
    void leiturasTemIndicePorRemessaEHorarioParaFiltrarEOrdenarJuntos() throws SQLException {
        assertEquals(List.of("REMESSA_ID", "REGISTRADA_EM"),
            colunasDoIndice("leitura_temperatura", "idx_leitura_remessa_horario"));
    }

    @Test
    void requisicoesTemIndicePorUsuario() throws SQLException {
        assertEquals(List.of("USUARIO_ID"), colunasDoIndice("requisicao", "idx_requisicao_usuario"));
    }

    @Test
    void bolsasDaRequisicaoTemIndicePorRequisicao() throws SQLException {
        assertEquals(List.of("REQUISICAO_ID"),
            colunasDoIndice("requisicao_bolsa", "idx_requisicao_bolsa_requisicao"));
    }

    /** Colunas do índice na ordem em que aparecem nele (vazio se o índice não existir). */
    private List<String> colunasDoIndice(String tabela, String indice) throws SQLException {
        try (var conexao = dataSource.getConnection()) {
            DatabaseMetaData metadados = conexao.getMetaData();
            String nomeArmazenado = metadados.storesLowerCaseIdentifiers()
                ? tabela.toLowerCase() : tabela.toUpperCase();
            List<String> colunas = new ArrayList<>();
            try (ResultSet linhas = metadados.getIndexInfo(null, null, nomeArmazenado, false, false)) {
                while (linhas.next()) {
                    if (indice.equalsIgnoreCase(linhas.getString("INDEX_NAME"))) {
                        colunas.add(linhas.getString("COLUMN_NAME").toUpperCase());
                    }
                }
            }
            return colunas;
        }
    }
}
