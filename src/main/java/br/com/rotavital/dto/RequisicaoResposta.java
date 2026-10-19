package br.com.rotavital.dto;

import java.util.List;
import br.com.rotavital.model.*;

public record RequisicaoResposta(
    Long id, String hospital, TipoComponente componente, TipoSanguineo tipoSanguineo,
    int quantidade, StatusRequisicao status, List<String> codigosBolsas
) {
    public static RequisicaoResposta de(Requisicao requisicao) {
        return new RequisicaoResposta(requisicao.getId(), requisicao.getHospital(),
            requisicao.getComponente(), requisicao.getTipoSanguineo(),
            requisicao.getQuantidade(), requisicao.getStatus(),
            requisicao.getBolsas().stream().map(BolsaHemoComponente::getCodigo).toList());
    }
}
