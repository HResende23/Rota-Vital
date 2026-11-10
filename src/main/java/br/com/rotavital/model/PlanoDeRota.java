package br.com.rotavital.model;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Rota confirmada para a remessa: caminho escolhido, horários previstos e janela do hospital. */
@Embeddable
public class PlanoDeRota {
    @Column(length = 40)
    private String destinoCodigo;
    @Column(length = 120)
    private String destinoNome;
    @Column(length = 200)
    private String caminhoChave;
    @Column(length = 400)
    private String caminhoDescricao;
    private Integer duracaoMinutos;
    private LocalDateTime saidaPrevista;
    private LocalDateTime chegadaPrevista;
    private LocalDateTime inicioAtendimento;
    private LocalDateTime fimAtendimento;
    private LocalDateTime janelaInicio;
    private LocalDateTime janelaFim;
    private Integer tempoTotalMinutos;
    private Boolean horarioDePico;
    private LocalDateTime planejadoEm;

    protected PlanoDeRota() {}

    public PlanoDeRota(String destinoCodigo, String destinoNome, String caminhoChave,
                       String caminhoDescricao, int duracaoMinutos, LocalDateTime saidaPrevista,
                       LocalDateTime chegadaPrevista, LocalDateTime inicioAtendimento,
                       LocalDateTime fimAtendimento, LocalDateTime janelaInicio,
                       LocalDateTime janelaFim, int tempoTotalMinutos, boolean horarioDePico,
                       LocalDateTime planejadoEm) {
        this.destinoCodigo = Objects.requireNonNull(destinoCodigo);
        this.destinoNome = Objects.requireNonNull(destinoNome);
        this.caminhoChave = Objects.requireNonNull(caminhoChave);
        this.caminhoDescricao = Objects.requireNonNull(caminhoDescricao);
        this.duracaoMinutos = duracaoMinutos;
        this.saidaPrevista = Objects.requireNonNull(saidaPrevista);
        this.chegadaPrevista = Objects.requireNonNull(chegadaPrevista);
        this.inicioAtendimento = Objects.requireNonNull(inicioAtendimento);
        this.fimAtendimento = Objects.requireNonNull(fimAtendimento);
        this.janelaInicio = Objects.requireNonNull(janelaInicio);
        this.janelaFim = Objects.requireNonNull(janelaFim);
        this.tempoTotalMinutos = tempoTotalMinutos;
        this.horarioDePico = horarioDePico;
        this.planejadoEm = Objects.requireNonNull(planejadoEm);
    }

    public String getDestinoCodigo() { return destinoCodigo; }
    public String getDestinoNome() { return destinoNome; }
    public String getCaminhoChave() { return caminhoChave; }
    public String getCaminhoDescricao() { return caminhoDescricao; }
    public int getDuracaoMinutos() { return duracaoMinutos; }
    public LocalDateTime getSaidaPrevista() { return saidaPrevista; }
    public LocalDateTime getChegadaPrevista() { return chegadaPrevista; }
    public LocalDateTime getInicioAtendimento() { return inicioAtendimento; }
    public LocalDateTime getFimAtendimento() { return fimAtendimento; }
    public LocalDateTime getJanelaInicio() { return janelaInicio; }
    public LocalDateTime getJanelaFim() { return janelaFim; }
    public int getTempoTotalMinutos() { return tempoTotalMinutos; }
    public boolean isHorarioDePico() { return Boolean.TRUE.equals(horarioDePico); }
    public LocalDateTime getPlanejadoEm() { return planejadoEm; }
}
