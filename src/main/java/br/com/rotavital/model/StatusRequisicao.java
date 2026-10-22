package br.com.rotavital.model;

public enum StatusRequisicao {
    PENDENTE_ALOCACAO("Pendente de alocação"),
    ALOCADA("Alocada");

    private final String rotulo;
    StatusRequisicao(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
