package br.com.rotavital.dto;

import jakarta.validation.constraints.*;

public record NovoUser(
    @NotBlank @Size(max = 200) String nome,
    @NotBlank @Email @Size(max = 254) String email
) {}
