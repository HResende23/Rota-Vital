package br.com.rotavital.model;

public enum TipoSanguineo {
    A_POSITIVO("A+"),
    A_NEGATIVO("A−"),
    B_POSITIVO("B+"),
    B_NEGATIVO("B−"),
    AB_POSITIVO("AB+"),
    AB_NEGATIVO("AB−"),
    O_POSITIVO("O+"),
    O_NEGATIVO("O−");

    private final String rotulo;
    TipoSanguineo(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
