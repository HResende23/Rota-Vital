package br.com.rotavital.controller;

import java.security.Principal;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import br.com.rotavital.service.UserService;
import br.com.rotavital.dto.RequisicaoResposta;

@Component
public class UsuarioDaSessao {
    private final UserService users;
    public UsuarioDaSessao(UserService users) { this.users = users; }
    public Long id(Principal principal) { return users.atual(principal.getName()).id(); }
    public void exigirDono(RequisicaoResposta requisicao, Principal principal) {
        if (!id(principal).equals(requisicao.usuarioId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requisição pertence a outro usuário");
        }
    }
}
