package br.com.rotavital.model;

/** Ligação de mão dupla entre dois pontos da malha, com duração em trânsito livre. */
public record Trecho(String origem, String destino, int minutosLivres, TipoVia via) {
    public Trecho {
        if (origem.equals(destino) || minutosLivres <= 0) {
            throw new IllegalArgumentException("Trecho inválido: " + origem + "-" + destino);
        }
    }

    public boolean liga(String ponto) {
        return origem.equals(ponto) || destino.equals(ponto);
    }

    public String outraPonta(String ponto) {
        if (!liga(ponto)) {
            throw new IllegalArgumentException("O trecho não passa por " + ponto);
        }
        return origem.equals(ponto) ? destino : origem;
    }

    /** Duração do trecho, aplicando o fator de pico do tipo de via quando for o caso. */
    public int duracao(boolean horarioDePico) {
        return horarioDePico ? (int) Math.round(minutosLivres * via.getFatorPico()) : minutosLivres;
    }

    public String getChave() {
        return origem + "-" + destino;
    }
}
