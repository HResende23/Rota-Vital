package br.com.rotavital.model;

/**
 * Hemocomponentes atendidos pelo protótipo. Cada um conhece a faixa térmica
 * usada no monitoramento da remessa (valores didáticos).
 */
public enum TipoComponente {
    CONCENTRADO_HEMACIAS("Concentrado de hemácias", new FaixaTermica(2.0, 6.0)),
    PLASMA("Plasma", new FaixaTermica(-30.0, -18.0)),
    PLAQUETAS("Plaquetas", new FaixaTermica(20.0, 24.0));

    private final String rotulo;
    private final FaixaTermica faixaTermica;

    TipoComponente(String rotulo, FaixaTermica faixaTermica) {
        this.rotulo = rotulo;
        this.faixaTermica = faixaTermica;
    }

    public String getRotulo() { return rotulo; }
    public FaixaTermica getFaixaTermica() { return faixaTermica; }
}
