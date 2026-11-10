package br.com.rotavital.model;

/** Situação do sinal do sensor no momento da leitura. É informativa e não altera a regra térmica. */
public enum Conectividade {
    CONECTADA("Conectada"),
    INSTAVEL("Sinal instável");

    private final String rotulo;
    Conectividade(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
