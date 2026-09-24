package br.com.rotavital.model;

import java.time.LocalDate;




public class BolsaHemoComponente {

    private String codigo;
    private TipoComponente componente;
    private TipoSanguineo tipoSanguineo;
    private LocalDate validade;
    private StatusBolsa status;

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


