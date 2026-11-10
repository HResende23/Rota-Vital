package br.com.rotavital.dto;

import br.com.rotavital.model.Conectividade;
import jakarta.validation.constraints.*;

/** Leitura enviada pelo sensor (API) ou registrada na tela como simulação do sensor. */
public record NovaLeitura(
    @NotNull @DecimalMin("-50.0") @DecimalMax("60.0") Double temperatura,
    Conectividade conectividade
) {}
