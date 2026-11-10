package br.com.rotavital.dto;

import br.com.rotavital.model.Remessa;
import br.com.rotavital.model.StatusRemessa;
import br.com.rotavital.model.TipoComponente;

/** Linha de listagem de remessas, sem carregar as leituras do sensor. */
public record RemessaResumo(Long id, String codigo, Long requisicaoId, String hospital,
                            TipoComponente componente, int quantidadeBolsas, StatusRemessa status,
                            boolean emAlerta) {
    public static RemessaResumo de(Remessa remessa) {
        var requisicao = remessa.getRequisicao();
        return new RemessaResumo(remessa.getId(), remessa.getCodigo(), requisicao.getId(),
            requisicao.getHospital(), requisicao.getComponente(), requisicao.getQuantidade(),
            remessa.getStatus(), remessa.isEmAlerta());
    }

    public String getRotulo() {
        return codigo + " · " + hospital + " · " + quantidadeBolsas
            + (quantidadeBolsas == 1 ? " bolsa · " : " bolsas · ") + status.getRotulo();
    }
}
