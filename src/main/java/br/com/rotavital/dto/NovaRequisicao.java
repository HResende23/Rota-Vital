package br.com.rotavital.dto;

import br.com.rotavital.model.*;
import jakarta.validation.constraints.*;

public record NovaRequisicao(
    @NotNull @Positive Long usuarioId,
    @NotBlank @Size(max = 200) String hospital,
    @NotNull TipoComponente componente,
    @NotNull TipoSanguineo tipoSanguineo,
    @Min(1) @Max(1000) int quantidade
) {}
