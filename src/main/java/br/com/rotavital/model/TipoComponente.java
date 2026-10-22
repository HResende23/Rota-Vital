package br.com.rotavital.model;

public enum TipoComponente {
    CONCENTRADO_HEMACIAS("Concentrado de hemácias"),
    PLASMA("Plasma"),
    PLAQUETAS("Plaquetas");

    private final String rotulo;
    TipoComponente(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
