package br.com.rotavital.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class BolsaHemoComponente {

    @Id
    @NotBlank(message = "O código da bolsa é obrigatório")
    private String codigo;

    @NotNull(message = "O tipo de componente é obrigatório")
    @Enumerated(EnumType.STRING)
    private TipoComponente componente;

    @NotNull(message = "O tipo sanguíneo é obrigatório")
    @Enumerated(EnumType.STRING)
    private TipoSanguineo tipoSanguineo;

    @NotNull(message = "A validade é obrigatória")
    @FutureOrPresent(message = "A validade não pode estar no passado")
    private LocalDate validade;

    @NotNull(message = "O status da bolsa é obrigatório")
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