package br.com.rotavital.dto;

import java.util.List;
import br.com.rotavital.model.BolsaHemoComponente;

public record PropostaAlocacao(
    Long requisicaoId,
    int solicitadas,
    int selecionadas,
    int faltantes,
    List<BolsaHemoComponente> bolsas
) {}
