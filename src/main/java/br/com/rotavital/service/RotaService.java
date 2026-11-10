package br.com.rotavital.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import br.com.rotavital.dto.*;
import br.com.rotavital.model.*;
import br.com.rotavital.repository.RemessaRepository;

/**
 * Planejamento de rota da remessa sobre a malha sintética: compara caminhos, aplica as regras de
 * viabilidade e propõe o caminho viável de menor duração.
 */
@Service
public class RotaService {
    public static final int MAXIMO_DE_TRECHOS = 4;
    public static final int MAXIMO_DE_ALTERNATIVAS = 3;

    private final RemessaRepository remessas;
    private final MalhaViaria malha;
    private final Clock relogio;

    public RotaService(RemessaRepository remessas, MalhaViaria malha, Clock relogio) {
        this.remessas = remessas;
        this.malha = malha;
        this.relogio = relogio;
    }

    /** Compara os caminhos sem alterar a remessa. */
    @Transactional(readOnly = true)
    public PlanejamentoRota simular(Long remessaId, ParametrosRota parametros) {
        Remessa remessa = remessas.findById(remessaId).orElseThrow(this::naoEncontrada);
        exigirPlanejavel(remessa);
        return planejar(remessa, parametros);
    }

    /**
     * Recalcula a simulação no servidor e grava o caminho escolhido (ou a proposta, quando nenhum
     * caminho é informado). Um caminho inviável nunca é aceito.
     */
    @Transactional
    public RemessaResposta confirmar(Long remessaId, ParametrosRota parametros, String caminho) {
        Remessa remessa = remessas.buscarParaAtualizacao(remessaId).orElseThrow(this::naoEncontrada);
        exigirPlanejavel(remessa);
        PlanejamentoRota planejamento = planejar(remessa, parametros);
        AlternativaRota escolhida = escolher(planejamento, caminho);
        remessa.planejar(new PlanoDeRota(planejamento.destino().codigo(), planejamento.destino().nome(),
            escolhida.chave(), escolhida.descricao(), escolhida.duracaoMinutos(), escolhida.saida(),
            escolhida.chegada(), escolhida.inicioAtendimento(), escolhida.fimAtendimento(),
            planejamento.janelaInicio(), planejamento.janelaFim(), escolhida.tempoTotalMinutos(),
            planejamento.horarioDePico(), agora()));
        remessas.flush();
        return RemessaResposta.de(remessa);
    }

    /**
     * Preenche o formulário: hospital reconhecido pelo nome digitado na requisição (ou o plano
     * atual), saída nos próximos minutos e uma janela de recebimento de uma hora.
     */
    public ParametrosRota sugerir(RemessaResposta remessa, String destino, LocalDateTime saida,
                                  LocalTime janelaInicio, LocalTime janelaFim) {
        PlanoDeRota plano = remessa.plano();
        LocalDateTime agora = agora();
        String destinoSugerido = destino != null && !destino.isBlank() ? destino
            : plano != null ? plano.getDestinoCodigo()
            : malha.hospitalCorrespondente(remessa.hospital()).map(PontoDaMalha::codigo).orElse(null);
        LocalDateTime saidaSugerida = saida != null ? saida
            : plano != null && plano.getSaidaPrevista().isAfter(agora) ? plano.getSaidaPrevista()
            : arredondarParaCima(agora.plusMinutes(15), 5);
        LocalTime inicio = janelaInicio != null ? janelaInicio
            : plano != null ? plano.getJanelaInicio().toLocalTime()
            : arredondarParaCima(saidaSugerida.plusMinutes(30), 15).toLocalTime();
        LocalTime fim = janelaFim != null ? janelaFim
            : plano != null ? plano.getJanelaFim().toLocalTime()
            : inicio.isAfter(LocalTime.of(22, 59)) ? LocalTime.of(23, 59) : inicio.plusHours(1);
        return new ParametrosRota(destinoSugerido, saidaSugerida, inicio, fim);
    }

    private PlanejamentoRota planejar(Remessa remessa, ParametrosRota parametros) {
        validar(parametros);
        PontoDaMalha destino = malha.ponto(parametros.destino()).filter(PontoDaMalha::isHospital)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Destino fora da malha sintética"));
        boolean pico = HorarioDePico.em(parametros.saida().toLocalTime());
        LocalDateTime janelaInicio = parametros.inicioDaJanela();
        LocalDateTime janelaFim = parametros.fimDaJanela();
        ViabilidadeDeRota.Carga carga = carga(remessa);
        List<AlternativaRota> avaliadas = malha
            .caminhos(MalhaViaria.HEMOCENTRO, destino.codigo(), MAXIMO_DE_TRECHOS).stream()
            .map(c -> ViabilidadeDeRota.avaliar(c, parametros.saida(), janelaInicio, janelaFim, pico, carga))
            .sorted(Comparator.comparingInt(AlternativaRota::duracaoMinutos)
                .thenComparingInt(a -> a.pontos().size())
                .thenComparing(AlternativaRota::chave))
            .limit(MAXIMO_DE_ALTERNATIVAS)
            .toList();
        AlternativaRota proposta = avaliadas.stream().filter(AlternativaRota::viavel).findFirst()
            .map(AlternativaRota::comoProposta).orElse(null);
        List<AlternativaRota> alternativas = avaliadas.stream()
            .map(a -> proposta != null && a.chave().equals(proposta.chave()) ? proposta : a)
            .toList();
        return new PlanejamentoRota(RemessaResposta.de(remessa), malha.hemocentro(), destino, parametros,
            janelaInicio, janelaFim, pico, alternativas, proposta);
    }

    private void validar(ParametrosRota p) {
        if (p == null || p.destino() == null || p.destino().isBlank() || p.saida() == null
                || p.janelaInicio() == null || p.janelaFim() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Informe o hospital de destino, a saída e a janela de recebimento");
        }
        if (!p.janelaFim().isAfter(p.janelaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A janela de recebimento deve terminar depois de começar");
        }
        if (p.saida().isBefore(agora().minusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A saída prevista não pode estar no passado");
        }
    }

    private AlternativaRota escolher(PlanejamentoRota planejamento, String caminho) {
        if (caminho == null || caminho.isBlank()) {
            if (planejamento.proposta() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, planejamento.alternativas().isEmpty()
                    ? "Não há caminho até o destino na malha"
                    : "Nenhum caminho atende aos critérios: " + motivos(planejamento.alternativas().get(0)));
            }
            return planejamento.proposta();
        }
        AlternativaRota escolhida = planejamento.alternativas().stream()
            .filter(a -> a.chave().equals(caminho)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Caminho não encontrado entre as alternativas simuladas"));
        if (!escolhida.viavel()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "O caminho escolhido não atende aos critérios: " + motivos(escolhida));
        }
        return escolhida;
    }

    private static String motivos(AlternativaRota alternativa) {
        return alternativa.getCriteriosNaoAtendidos().stream()
            .map(c -> c.nome().toLowerCase()).collect(Collectors.joining(", "));
    }

    private static ViabilidadeDeRota.Carga carga(Remessa remessa) {
        List<BolsaHemoComponente> bolsas = remessa.getRequisicao().getBolsas();
        BolsaHemoComponente maisProxima = bolsas.stream()
            .min(Comparator.comparing(BolsaHemoComponente::getValidade)
                .thenComparing(BolsaHemoComponente::getCodigo))
            .orElse(null);
        return new ViabilidadeDeRota.Carga(bolsas.size(), remessa.getCaixa(), remessa.getQuantidadeCaixas(),
            maisProxima == null ? null : maisProxima.getCodigo(),
            maisProxima == null ? null : maisProxima.getValidade());
    }

    private static void exigirPlanejavel(Remessa remessa) {
        try {
            remessa.exigirPlanejavel();
        } catch (IllegalStateException erro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, erro.getMessage());
        }
    }

    static LocalDateTime arredondarParaCima(LocalDateTime horario, int minutos) {
        LocalDateTime base = horario.truncatedTo(ChronoUnit.MINUTES);
        if (base.isBefore(horario)) {
            base = base.plusMinutes(1);
        }
        int resto = base.getMinute() % minutos;
        return resto == 0 ? base : base.plusMinutes(minutos - resto);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
    }

    private ResponseStatusException naoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Remessa não encontrada");
    }
}
