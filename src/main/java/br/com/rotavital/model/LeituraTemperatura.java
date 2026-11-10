package br.com.rotavital.model;

import java.time.LocalDateTime;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.*;

/**
 * Leitura do sensor da caixa térmica. É criada somente pela própria remessa.
 * O índice (remessa, horário) atende o filtro por remessa e a ordem cronológica das leituras
 * na mesma varredura, além da exclusão em cascata quando a remessa é apagada.
 */
@Entity
@Table(name = "leitura_temperatura",
    indexes = @Index(name = "idx_leitura_remessa_horario", columnList = "remessa_id, registrada_em"))
public class LeituraTemperatura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remessa_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Remessa remessa;

    @Column(nullable = false)
    private LocalDateTime registradaEm;

    @Column(nullable = false)
    private double temperatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Conectividade conectividade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CondicaoLeitura condicao;

    protected LeituraTemperatura() {}

    LeituraTemperatura(Remessa remessa, LocalDateTime registradaEm, double temperatura,
                       Conectividade conectividade, CondicaoLeitura condicao) {
        this.remessa = remessa;
        this.registradaEm = registradaEm;
        this.temperatura = temperatura;
        this.conectividade = conectividade;
        this.condicao = condicao;
    }

    public Long getId() { return id; }
    public LocalDateTime getRegistradaEm() { return registradaEm; }
    public double getTemperatura() { return temperatura; }
    public Conectividade getConectividade() { return conectividade; }
    public CondicaoLeitura getCondicao() { return condicao; }
}
