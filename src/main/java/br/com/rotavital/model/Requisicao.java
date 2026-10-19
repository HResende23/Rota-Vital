package br.com.rotavital.model;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

@Entity
public class Requisicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String hospital;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoComponente componente;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSanguineo tipoSanguineo;
    @Column(nullable = false)
    private int quantidade;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusRequisicao status = StatusRequisicao.PENDENTE_ALOCACAO;
    @ManyToMany
    @JoinTable(name = "requisicao_bolsa",
        joinColumns = @JoinColumn(name = "requisicao_id"),
        inverseJoinColumns = @JoinColumn(name = "bolsa_codigo"),
        uniqueConstraints = @UniqueConstraint(columnNames = "bolsa_codigo"))
    private List<BolsaHemoComponente> bolsas = new ArrayList<>();

    protected Requisicao() {}

    public Requisicao(String hospital, TipoComponente componente,
                      TipoSanguineo tipoSanguineo, int quantidade) {
        this.hospital = hospital;
        this.componente = componente;
        this.tipoSanguineo = tipoSanguineo;
        this.quantidade = quantidade;
    }

    public void alocar(List<BolsaHemoComponente> bolsas) {
        if (status != StatusRequisicao.PENDENTE_ALOCACAO || bolsas.size() != quantidade) {
            throw new IllegalStateException("Requisição não pode ser alocada");
        }
        this.bolsas.addAll(bolsas);
        this.status = StatusRequisicao.ALOCADA;
    }

    public Long getId() { return id; }
    public String getHospital() { return hospital; }
    public TipoComponente getComponente() { return componente; }
    public TipoSanguineo getTipoSanguineo() { return tipoSanguineo; }
    public int getQuantidade() { return quantidade; }
    public StatusRequisicao getStatus() { return status; }
    public List<BolsaHemoComponente> getBolsas() { return List.copyOf(bolsas); }
}
