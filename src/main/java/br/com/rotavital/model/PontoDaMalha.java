package br.com.rotavital.model;

/**
 * Ponto da malha viária sintética. As coordenadas x/y e a posição do rótulo servem apenas para
 * desenhar o mapa esquemático na tela de rota (não são coordenadas geográficas).
 */
public record PontoDaMalha(String codigo, String nome, TipoPonto tipo, int x, int y, PosicaoRotulo rotulo) {
    public boolean isHospital() {
        return tipo == TipoPonto.HOSPITAL;
    }

    public int getRotuloX() {
        return switch (rotulo) {
            case ESQUERDA -> x - 22;
            case DIREITA -> x + 22;
            default -> x;
        };
    }

    public int getRotuloY() {
        return switch (rotulo) {
            case ACIMA -> y - 24;
            case ABAIXO -> y + 40;
            default -> y + 6;
        };
    }

    public String getRotuloAncora() {
        return switch (rotulo) {
            case ESQUERDA -> "end";
            case DIREITA -> "start";
            default -> "middle";
        };
    }
}
