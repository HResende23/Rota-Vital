package br.com.rotavital.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import br.com.rotavital.dto.NovaLeitura;
import br.com.rotavital.dto.RemessaResposta;
import br.com.rotavital.dto.RemessaResumo;
import br.com.rotavital.model.Remessa;
import br.com.rotavital.model.ReservaConfirmada;
import br.com.rotavital.repository.RemessaRepository;
import br.com.rotavital.repository.RequisicaoRepository;

/**
 * Monitoramento térmico da remessa: começa automaticamente quando a reserva é confirmada
 * na alocação e termina com a entrega no hospital.
 */
@Service
public class MonitoramentoService {
    private final RemessaRepository remessas;
    private final RequisicaoRepository requisicoes;
    private final Clock relogio;

    public MonitoramentoService(RemessaRepository remessas, RequisicaoRepository requisicoes, Clock relogio) {
        this.remessas = remessas;
        this.requisicoes = requisicoes;
        this.relogio = relogio;
    }

    /** Executado de forma síncrona, na mesma transação da reserva: se falhar, a reserva é desfeita. */
    @EventListener
    @Transactional
    public void aoConfirmarReserva(ReservaConfirmada evento) {
        iniciar(evento.requisicaoId());
    }

    @Transactional
    public RemessaResposta iniciar(Long requisicaoId) {
        if (remessas.existsByRequisicaoId(requisicaoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "O monitoramento desta requisição já foi iniciado");
        }
        var requisicao = requisicoes.findById(requisicaoId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));
        Remessa remessa;
        try {
            remessa = Remessa.iniciarMonitoramento(requisicao, agora());
        } catch (IllegalStateException erro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, erro.getMessage());
        }
        return RemessaResposta.de(remessas.save(remessa));
    }

    @Transactional(readOnly = true)
    public List<RemessaResumo> listarDoUsuario(Long usuarioId) {
        return remessas.listarDoUsuario(usuarioId).stream().map(RemessaResumo::de).toList();
    }

    @Transactional(readOnly = true)
    public RemessaResposta consultar(Long id) {
        return RemessaResposta.de(remessas.findById(id).orElseThrow(this::naoEncontrada));
    }

    @Transactional(readOnly = true)
    public Optional<RemessaResumo> daRequisicao(Long requisicaoId) {
        return remessas.findByRequisicaoId(requisicaoId).map(RemessaResumo::de);
    }

    @Transactional
    public RemessaResposta registrarLeitura(Long id, NovaLeitura dados) {
        return alterar(id, remessa ->
            remessa.registrarLeitura(dados.temperatura(), dados.conectividade(), agora()));
    }

    @Transactional
    public RemessaResposta reconhecerAlerta(Long id, String responsavel) {
        return alterar(id, remessa -> remessa.reconhecerAlerta(responsavel, agora()));
    }

    @Transactional
    public RemessaResposta iniciarTransporte(Long id) {
        return alterar(id, remessa -> remessa.iniciarTransporte(agora()));
    }

    @Transactional
    public RemessaResposta registrarEntrega(Long id) {
        return alterar(id, remessa -> remessa.registrarEntrega(agora()));
    }

    /** Bloqueia a remessa, aplica a regra de domínio e traduz violações em respostas HTTP. */
    private RemessaResposta alterar(Long id, Consumer<Remessa> acao) {
        Remessa remessa = remessas.buscarParaAtualizacao(id).orElseThrow(this::naoEncontrada);
        try {
            acao.accept(remessa);
        } catch (IllegalStateException erro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, erro.getMessage());
        } catch (IllegalArgumentException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erro.getMessage());
        }
        remessas.flush();
        return RemessaResposta.de(remessa);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
    }

    private ResponseStatusException naoEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Remessa não encontrada");
    }
}
