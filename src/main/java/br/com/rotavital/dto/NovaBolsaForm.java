package br.com.rotavital.dto;

import java.time.LocalDate;
import br.com.rotavital.model.*;
import jakarta.validation.constraints.*;

public record NovaBolsaForm(
    @NotBlank @Size(max = 100) String codigo,
    @NotNull TipoComponente componente,
    @NotNull TipoSanguineo tipoSanguineo,
    @NotNull @FutureOrPresent LocalDate validade
) {}
