package br.com.rotavital.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

@Entity
public class BolsaHemoComponente {

    @Id
    private String codigo;

    @Enumerated(EnumType.STRING)
    private TipoComponente componente;

    @Enumerated(EnumType.STRING)
    private TipoSanguineo tipoSanguineo;

    private LocalDate validade;

    @Enumerated(EnumType.STRING)
    private StatusBolsa status;

    public BolsaHemoComponente() {
    }

    public BolsaHemoComponente(
            String codigo,
            TipoComponente componente,
            TipoSanguineo tipoSanguineo,
            LocalDate validade,
            StatusBolsa status) {

        this.codigo = codigo;
        this.componente = componente;
        this.tipoSanguineo = tipoSanguineo;
        this.validade = validade;
        this.status = status;
    }

    public boolean estaVencida() {
        return validade.isBefore(LocalDate.now());
    }

    public boolean estaDisponivel() {
        return status == StatusBolsa.DISPONIVEL
                && !estaVencida();
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoComponente getComponente() {
        return componente;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public StatusBolsa getStatus() {
        return status;
    }
}