package br.com.rotavital.dto;

import java.time.LocalDateTime;
import br.com.rotavital.model.Conectividade;
import br.com.rotavital.model.CondicaoLeitura;
import br.com.rotavital.model.LeituraTemperatura;

public record LeituraResposta(Long id, LocalDateTime registradaEm, double temperatura,
                              Conectividade conectividade, CondicaoLeitura condicao) {
    public static LeituraResposta de(LeituraTemperatura leitura) {
        return new LeituraResposta(leitura.getId(), leitura.getRegistradaEm(), leitura.getTemperatura(),
            leitura.getConectividade(), leitura.getCondicao());
    }

    public boolean isForaDaFaixa() {
        return condicao.foraDaFaixa();
    }
}
