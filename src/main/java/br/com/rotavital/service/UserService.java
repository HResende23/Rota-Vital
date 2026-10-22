package br.com.rotavital.service;

import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import br.com.rotavital.dto.*;
import br.com.rotavital.model.User;
import br.com.rotavital.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) { this.users = users; }

    @Transactional
    public UserResposta cadastrar(NovoUser dados) {
        String email = dados.email().strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw emailDuplicado();
        }
        try {
            return UserResposta.de(users.saveAndFlush(new User(dados.nome(), email)));
        } catch (DataIntegrityViolationException erro) {
            // A restrição no banco também protege cadastros simultâneos.
            throw emailDuplicado();
        }
    }

    @Transactional(readOnly = true)
    public UserResposta consultar(Long id) {
        return UserResposta.de(users.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado")));
    }

    private ResponseStatusException emailDuplicado() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
    }
}
