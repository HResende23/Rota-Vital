package br.com.rotavital.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.*;

/**
 * Remessa de bolsas reservadas para uma requisição. Nasce na confirmação da reserva,
 * quando o sensor da caixa térmica passa a ser monitorado, e acompanha o planejamento
 * da rota, a saída do hemocentro e a entrega no hospital.
 */
@Entity
@Table(name = "remessa")
public class Remessa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisicao_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Requisicao requisicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusRemessa status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModeloCaixa caixa;

    @Column(nullable = false)
    private int quantidadeCaixas;

    // A faixa é copiada na abertura para preservar o histórico mesmo se a regra mudar.
    @Column(nullable = false)
    private double temperaturaMinima;

    @Column(nullable = false)
    private double temperaturaMaxima;

    @Column(nullable = false)
    private LocalDateTime monitoramentoIniciadoEm;

    private LocalDateTime monitoramentoEncerradoEm;

    /** Primeira leitura da sequência atual fora da faixa; nulo quando a última leitura está na faixa. */
    private LocalDateTime desvioIniciadoEm;

    private LocalDateTime alertaDesvioIniciadoEm;
    private LocalDateTime alertaEmitidoEm;
    private LocalDateTime alertaReconhecidoEm;

    @Column(length = 254)
    private String alertaReconhecidoPor;

    private LocalDateTime saidaEm;
    private LocalDateTime entregueEm;

    @Embedded
    private PlanoDeRota plano;

    @OneToMany(mappedBy = "remessa", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("registradaEm ASC, id ASC")
    private List<LeituraTemperatura> leituras = new ArrayList<>();

    protected Remessa() {}

    private Remessa(Requisicao requisicao, LocalDateTime inicio) {
        int bolsas = requisicao.getBolsas().size();
        FaixaTermica faixa = requisicao.getComponente().getFaixaTermica();
        this.requisicao = requisicao;
        this.status = StatusRemessa.AGUARDANDO_ROTA;
        this.caixa = ModeloCaixa.para(bolsas);
        this.quantidadeCaixas = caixa.caixasNecessarias(bolsas);
        this.temperaturaMinima = faixa.minima();
        this.temperaturaMaxima = faixa.maxima();
        this.monitoramentoIniciadoEm = inicio;
    }

    /** Abre a remessa de uma requisição recém-alocada e inicia o monitoramento térmico. */
    public static Remessa iniciarMonitoramento(Requisicao requisicao, LocalDateTime inicio) {
        Objects.requireNonNull(requisicao, "Requisição obrigatória");
        Objects.requireNonNull(inicio, "Horário de início obrigatório");
        if (requisicao.getStatus() != StatusRequisicao.ALOCADA || requisicao.getBolsas().isEmpty()) {
            throw new IllegalStateException("O monitoramento só começa após a reserva das bolsas");
        }
        return new Remessa(requisicao, inicio);
    }

    /**
     * Registra uma leitura do sensor. Uma leitura fora da faixa abre um desvio; a leitura
     * seguinte também fora da faixa confirma o alerta térmico, bloqueia as bolsas e deixa a
     * remessa pendente de avaliação. Voltar à faixa encerra o desvio, mas não desfaz o alerta.
     */
    public LeituraTemperatura registrarLeitura(double temperatura, Conectividade conectividade,
                                               LocalDateTime horario) {
        Objects.requireNonNull(horario, "Horário da leitura obrigatório");
        if (!status.monitorada()) {
            throw new IllegalStateException("Monitoramento encerrado: a remessa já foi entregue");
        }
        if (!Double.isFinite(temperatura)) {
            throw new IllegalArgumentException("Temperatura inválida");
        }
        LeituraTemperatura ultima = getUltimaLeitura();
        if (ultima != null && horario.isBefore(ultima.getRegistradaEm())) {
            throw new IllegalStateException("A leitura não pode ser anterior à última registrada");
        }
        CondicaoLeitura condicao = classificar(temperatura, horario);
        var leitura = new LeituraTemperatura(this, horario, temperatura,
            conectividade == null ? Conectividade.CONECTADA : conectividade, condicao);
        leituras.add(leitura);
        return leitura;
    }

    private CondicaoLeitura classificar(double temperatura, LocalDateTime horario) {
        if (getFaixa().contem(temperatura)) {
            desvioIniciadoEm = null;
            return CondicaoLeitura.DENTRO_DA_FAIXA;
        }
        if (desvioIniciadoEm == null) {
            desvioIniciadoEm = horario;
            return CondicaoLeitura.INICIO_DO_DESVIO;
        }
        if (alertaEmitidoEm == null) {
            emitirAlerta(horario);
        }
        return CondicaoLeitura.ALERTA_TERMICO;
    }

    private void emitirAlerta(LocalDateTime horario) {
        alertaDesvioIniciadoEm = desvioIniciadoEm;
        alertaEmitidoEm = horario;
        status = StatusRemessa.PENDENTE_AVALIACAO;
        requisicao.getBolsas().forEach(BolsaHemoComponente::bloquear);
    }

    /** Registra a ciência do alerta. As bolsas continuam bloqueadas até uma avaliação técnica. */
    public void reconhecerAlerta(String responsavel, LocalDateTime horario) {
        if (alertaEmitidoEm == null) {
            throw new IllegalStateException("Não há alerta térmico para reconhecer");
        }
        if (alertaReconhecidoEm != null) {
            throw new IllegalStateException("O alerta já foi reconhecido");
        }
        this.alertaReconhecidoPor = Objects.requireNonNull(responsavel, "Responsável obrigatório");
        this.alertaReconhecidoEm = Objects.requireNonNull(horario, "Horário obrigatório");
    }

    /** Confirma (ou substitui, antes da saída) o plano de rota da remessa. */
    public void planejar(PlanoDeRota novoPlano) {
        Objects.requireNonNull(novoPlano, "Plano obrigatório");
        exigirPlanejavel();
        this.plano = novoPlano;
        this.status = StatusRemessa.PLANEJADA;
    }

    /** A rota só pode ser (re)planejada antes da saída e sem alerta térmico pendente. */
    public void exigirPlanejavel() {
        if (!status.permitePlanejamento()) {
            throw new IllegalStateException(motivoSemPlanejamento());
        }
    }

    public void iniciarTransporte(LocalDateTime horario) {
        Objects.requireNonNull(horario, "Horário obrigatório");
        if (status == StatusRemessa.AGUARDANDO_ROTA) {
            throw new IllegalStateException("Planeje a rota antes de iniciar o transporte");
        }
        if (status != StatusRemessa.PLANEJADA) {
            throw new IllegalStateException(motivoSemPlanejamento());
        }
        this.saidaEm = horario;
        this.status = StatusRemessa.EM_TRANSPORTE;
    }

    public void registrarEntrega(LocalDateTime horario) {
        Objects.requireNonNull(horario, "Horário obrigatório");
        if (status != StatusRemessa.EM_TRANSPORTE) {
            throw new IllegalStateException("Somente uma remessa em transporte pode ser entregue");
        }
        this.entregueEm = horario;
        this.monitoramentoEncerradoEm = horario;
        this.desvioIniciadoEm = null;
        this.status = StatusRemessa.ENTREGUE;
    }

    private String motivoSemPlanejamento() {
        return switch (status) {
            case PENDENTE_AVALIACAO -> "Remessa com alerta térmico aguarda avaliação e não pode seguir para entrega";
            case EM_TRANSPORTE -> "A remessa já saiu do hemocentro";
            case ENTREGUE -> "A remessa já foi entregue";
            default -> "A remessa não pode ser planejada na situação atual";
        };
    }

    public String getCodigo() {
        return id == null ? "R-nova" : String.format("R-%03d", id);
    }

    public FaixaTermica getFaixa() {
        return new FaixaTermica(temperaturaMinima, temperaturaMaxima);
    }

    public LeituraTemperatura getUltimaLeitura() {
        return leituras.isEmpty() ? null : leituras.get(leituras.size() - 1);
    }

    public boolean isEmAlerta() { return alertaEmitidoEm != null; }

    public Long getId() { return id; }
    public Requisicao getRequisicao() { return requisicao; }
    public StatusRemessa getStatus() { return status; }
    public ModeloCaixa getCaixa() { return caixa; }
    public int getQuantidadeCaixas() { return quantidadeCaixas; }
    public LocalDateTime getMonitoramentoIniciadoEm() { return monitoramentoIniciadoEm; }
    public LocalDateTime getMonitoramentoEncerradoEm() { return monitoramentoEncerradoEm; }
    public LocalDateTime getDesvioIniciadoEm() { return desvioIniciadoEm; }
    public LocalDateTime getAlertaDesvioIniciadoEm() { return alertaDesvioIniciadoEm; }
    public LocalDateTime getAlertaEmitidoEm() { return alertaEmitidoEm; }
    public LocalDateTime getAlertaReconhecidoEm() { return alertaReconhecidoEm; }
    public String getAlertaReconhecidoPor() { return alertaReconhecidoPor; }
    public LocalDateTime getSaidaEm() { return saidaEm; }
    public LocalDateTime getEntregueEm() { return entregueEm; }
    public PlanoDeRota getPlano() { return plano; }
    public List<LeituraTemperatura> getLeituras() { return Collections.unmodifiableList(leituras); }
}
