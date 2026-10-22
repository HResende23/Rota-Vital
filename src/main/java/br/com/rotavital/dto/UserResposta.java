package br.com.rotavital.dto;

import br.com.rotavital.model.User;

public record UserResposta(Long id, String nome, String email) {
    public static UserResposta de(User user) {
        return new UserResposta(user.getId(), user.getNome(), user.getEmail());
    }
}
