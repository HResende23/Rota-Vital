package br.com.rotavital.model;

import java.time.LocalTime;

/** Faixas de horário de pico consideradas pela simulação: 07h–09h e 17h–19h. */
public final class HorarioDePico {
    private static final LocalTime MANHA_INICIO = LocalTime.of(7, 0);
    private static final LocalTime MANHA_FIM = LocalTime.of(9, 0);
    private static final LocalTime TARDE_INICIO = LocalTime.of(17, 0);
    private static final LocalTime TARDE_FIM = LocalTime.of(19, 0);

    private HorarioDePico() {}

    public static boolean em(LocalTime horario) {
        return dentro(horario, MANHA_INICIO, MANHA_FIM) || dentro(horario, TARDE_INICIO, TARDE_FIM);
    }

    private static boolean dentro(LocalTime horario, LocalTime inicio, LocalTime fim) {
        return !horario.isBefore(inicio) && horario.isBefore(fim);
    }
}
