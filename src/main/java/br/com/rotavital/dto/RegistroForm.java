package br.com.rotavital.dto;

import jakarta.validation.constraints.*;

public record RegistroForm(
    @NotBlank @Size(max = 200) String nome,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 72) String senha,
    @NotBlank String confirmacao
) {
    @AssertTrue(message = "As senhas precisam ser iguais.")
    public boolean isSenhasIguais() {
        return senha != null && senha.equals(confirmacao);
    }

    @AssertTrue(message = "A senha deve ter no máximo 72 bytes em UTF-8.")
    public boolean isSenhaCompativel() {
        return senha == null || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
    }
}
