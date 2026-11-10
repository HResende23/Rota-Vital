package br.com.rotavital.model;

/** Tipo de via do trecho. Vias urbanas ficam mais lentas no horário de pico. */
public enum TipoVia {
    URBANA("Via urbana", 1.5),
    EXPRESSA("Via expressa", 1.0);

    private final String rotulo;
    private final double fatorPico;

    TipoVia(String rotulo, double fatorPico) {
        this.rotulo = rotulo;
        this.fatorPico = fatorPico;
    }

    public String getRotulo() { return rotulo; }
    public double getFatorPico() { return fatorPico; }
}
