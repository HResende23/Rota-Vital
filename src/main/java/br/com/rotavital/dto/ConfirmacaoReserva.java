package br.com.rotavital.dto;

import java.util.List;
import jakarta.validation.constraints.*;

public record ConfirmacaoReserva(
    @NotEmpty @Size(max = 1000) List<@NotBlank String> codigosBolsas
) {}
