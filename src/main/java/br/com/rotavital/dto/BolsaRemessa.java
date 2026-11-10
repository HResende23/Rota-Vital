package br.com.rotavital.dto;

import java.time.LocalDate;
import br.com.rotavital.model.BolsaHemoComponente;
import br.com.rotavital.model.StatusBolsa;
import br.com.rotavital.model.TipoSanguineo;

public record BolsaRemessa(String codigo, TipoSanguineo tipoSanguineo, LocalDate validade, StatusBolsa status) {
    public static BolsaRemessa de(BolsaHemoComponente bolsa) {
        return new BolsaRemessa(bolsa.getCodigo(), bolsa.getTipoSanguineo(), bolsa.getValidade(), bolsa.getStatus());
    }
}
