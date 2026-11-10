package br.com.rotavital.model;

/**
 * Modelos de caixa térmica com sensor. A autonomia é contada a partir da saída
 * do hemocentro até o fim do atendimento no hospital.
 */
public enum ModeloCaixa {
    CAIXA_P("Caixa térmica P", 4, 90),
    CAIXA_M("Caixa térmica M", 8, 120),
    CAIXA_G("Caixa térmica G", 16, 150);

    private final String rotulo;
    private final int capacidade;
    private final int autonomiaMinutos;

    ModeloCaixa(String rotulo, int capacidade, int autonomiaMinutos) {
        this.rotulo = rotulo;
        this.capacidade = capacidade;
        this.autonomiaMinutos = autonomiaMinutos;
    }

    /** Menor caixa que comporta as bolsas; acima da maior capacidade usa várias caixas G. */
    public static ModeloCaixa para(int bolsas) {
        if (bolsas < 1) {
            throw new IllegalArgumentException("A remessa precisa de pelo menos uma bolsa");
        }
        for (ModeloCaixa modelo : values()) {
            if (bolsas <= modelo.capacidade) {
                return modelo;
            }
        }
        return CAIXA_G;
    }

    public int caixasNecessarias(int bolsas) {
        return (bolsas + capacidade - 1) / capacidade;
    }

    public String getRotulo() { return rotulo; }
    public int getCapacidade() { return capacidade; }
    public int getAutonomiaMinutos() { return autonomiaMinutos; }
}
