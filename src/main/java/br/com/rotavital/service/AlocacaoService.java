package br.com.rotavital.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import br.com.rotavital.dto.ConfirmacaoReserva;
import br.com.rotavital.dto.NovaRequisicao;
import br.com.rotavital.dto.PropostaAlocacao;
import br.com.rotavital.dto.RequisicaoResposta;
import br.com.rotavital.model.BolsaHemoComponente;
import br.com.rotavital.model.Requisicao;
import br.com.rotavital.model.StatusBolsa;
import br.com.rotavital.model.StatusRequisicao;
import br.com.rotavital.repository.BolsaHemoComponenteRepository;
import br.com.rotavital.repository.RequisicaoRepository;

@Service
public class AlocacaoService {
    private final RequisicaoRepository requisicoes;
    private final BolsaHemoComponenteRepository bolsas;
    private final CompatibilidadeService compatibilidade;

    public AlocacaoService(RequisicaoRepository requisicoes,
                           BolsaHemoComponenteRepository bolsas,
                           CompatibilidadeService compatibilidade) {
        this.requisicoes = requisicoes;
        this.bolsas = bolsas;
        this.compatibilidade = compatibilidade;
    }

    @Transactional
    public RequisicaoResposta cadastrar(NovaRequisicao dados) {
        return RequisicaoResposta.de(requisicoes.save(new Requisicao(dados.hospital(),
            dados.componente(), dados.tipoSanguineo(), dados.quantidade())));
    }

    @Transactional(readOnly = true)
    public RequisicaoResposta consultar(Long id) {
        return RequisicaoResposta.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public PropostaAlocacao propor(Long id) {
        Requisicao requisicao = buscar(id);
        exigirPendente(requisicao);
        List<BolsaHemoComponente> selecionadas = selecionarParaProposta(requisicao);
        return new PropostaAlocacao(id, requisicao.getQuantidade(), selecionadas.size(),
            requisicao.getQuantidade() - selecionadas.size(), selecionadas);
    }

    private List<BolsaHemoComponente> selecionarParaProposta(Requisicao requisicao) {
        return bolsas
            .findByComponenteAndStatusAndValidadeGreaterThanEqualOrderByValidadeAscCodigoAsc(
                requisicao.getComponente(), StatusBolsa.DISPONIVEL, LocalDate.now())
            .stream()
            .filter(b -> compativel(b, requisicao))
            .limit(requisicao.getQuantidade())
            .toList();
    }

    @Transactional
    public RequisicaoResposta confirmar(Long id, ConfirmacaoReserva dados) {
        // Bloqueia primeiro a requisição e depois as bolsas, sempre na ordem do código.
        Requisicao requisicao = requisicoes.buscarParaReserva(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));
        exigirPendente(requisicao);
        List<BolsaHemoComponente> selecionadas = buscarSelecaoParaReserva(
            dados.codigosBolsas(), requisicao.getQuantidade());
        exigirElegibilidade(selecionadas, requisicao);
        selecionadas.forEach(BolsaHemoComponente::reservar);
        requisicao.alocar(selecionadas);
        // JPA persiste os objetos gerenciados ao concluir a mesma transação.
        return RequisicaoResposta.de(requisicao);
    }

    private List<BolsaHemoComponente> buscarSelecaoParaReserva(List<String> codigos,
                                                             int quantidadeSolicitada) {
        if (codigos.size() != quantidadeSolicitada
                || new HashSet<>(codigos).size() != codigos.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Informe bolsas distintas na quantidade solicitada");
        }
        List<BolsaHemoComponente> selecionadas = bolsas.buscarParaReserva(codigos);
        if (selecionadas.size() != codigos.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bolsa não encontrada");
        }
        return selecionadas;
    }

    private void exigirElegibilidade(List<BolsaHemoComponente> selecionadas,
                                     Requisicao requisicao) {
        // Valida o lote inteiro antes de alterar qualquer estado.
        for (BolsaHemoComponente bolsa : selecionadas) {
            if (!bolsa.estaDisponivel() || !compativel(bolsa, requisicao)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A bolsa " + bolsa.getCodigo() + " não está mais elegível; consulte nova proposta");
            }
        }
    }

    private boolean compativel(BolsaHemoComponente bolsa, Requisicao requisicao) {
        return bolsa.getComponente() == requisicao.getComponente()
            && compatibilidade.compativel(requisicao.getComponente(),
                bolsa.getTipoSanguineo(), requisicao.getTipoSanguineo());
    }

    private Requisicao buscar(Long id) {
        return requisicoes.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));
    }

    private void exigirPendente(Requisicao requisicao) {
        if (requisicao.getStatus() != StatusRequisicao.PENDENTE_ALOCACAO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requisição já alocada");
        }
    }
}
