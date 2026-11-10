package br.com.rotavital.model;

public enum TipoPonto {
    HEMOCENTRO("Hemocentro"),
    APOIO("Ponto de apoio"),
    HOSPITAL("Hospital");

    private final String rotulo;
    TipoPonto(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
