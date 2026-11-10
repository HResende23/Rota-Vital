package br.com.rotavital.model;

/** Ciclo de vida da remessa, da reserva das bolsas até a entrega no hospital. */
public enum StatusRemessa {
    AGUARDANDO_ROTA("Aguardando rota"),
    PLANEJADA("Rota planejada"),
    EM_TRANSPORTE("Em transporte"),
    ENTREGUE("Entregue"),
    PENDENTE_AVALIACAO("Pendente de avaliação");

    private final String rotulo;
    StatusRemessa(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }

    /** A rota pode ser planejada ou replanejada até a saída do hemocentro. */
    public boolean permitePlanejamento() {
        return this == AGUARDANDO_ROTA || this == PLANEJADA;
    }

    /** O sensor da caixa é monitorado da reserva até a entrega. */
    public boolean monitorada() {
        return this != ENTREGUE;
    }
}
