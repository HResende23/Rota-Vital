package br.com.rotavital.model;

import java.util.Locale;

/** Faixa de temperatura aceita para um hemocomponente (valores didáticos, sem validação clínica). */
public record FaixaTermica(double minima, double maxima) {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    public FaixaTermica {
        if (!(minima < maxima)) {
            throw new IllegalArgumentException("A temperatura mínima deve ser menor que a máxima");
        }
    }

    public boolean contem(double temperatura) {
        return temperatura >= minima && temperatura <= maxima;
    }

    public String getRotulo() {
        return String.format(PT_BR, "%.1f a %.1f °C", minima, maxima);
    }
}
