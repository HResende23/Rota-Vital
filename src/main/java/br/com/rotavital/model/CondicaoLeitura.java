package br.com.rotavital.model;

/**
 * Classificação de cada leitura. A primeira leitura fora da faixa abre o desvio;
 * uma segunda leitura consecutiva fora da faixa confirma o alerta térmico.
 */
public enum CondicaoLeitura {
    DENTRO_DA_FAIXA("Dentro da faixa"),
    INICIO_DO_DESVIO("Início do desvio"),
    ALERTA_TERMICO("Alerta térmico");

    private final String rotulo;
    CondicaoLeitura(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }

    public boolean foraDaFaixa() {
        return this != DENTRO_DA_FAIXA;
    }
}
