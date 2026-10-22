package br.com.rotavital.controller;

import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import br.com.rotavital.dto.*;
import br.com.rotavital.service.UserService;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UserController {
    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<UserResposta> cadastrar(@Valid @RequestBody NovoUser dados) {
        UserResposta resposta = service.cadastrar(dados);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + resposta.id())).body(resposta);
    }

    @GetMapping("/{id}")
    public UserResposta consultar(@PathVariable Long id, java.security.Principal principal) {
        UserResposta resposta = service.consultar(id);
        if (!resposta.email().equals(principal.getName())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Acesso negado");
        }
        return resposta;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratarErro(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(Map.of("erro", erro.getReason()));
    }
}
