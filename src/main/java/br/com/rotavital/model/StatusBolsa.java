package br.com.rotavital.model;

public enum StatusBolsa {
    DISPONIVEL("Disponível"),
    RESERVADA("Reservada"),
    INDISPONIVEL("Indisponível");

    private final String rotulo;
    StatusBolsa(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
