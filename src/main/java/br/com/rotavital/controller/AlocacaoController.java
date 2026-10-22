package br.com.rotavital.controller;

import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import br.com.rotavital.dto.*;
import br.com.rotavital.service.AlocacaoService;

@RestController
@RequestMapping("/api/v1/requisicoes")
public class AlocacaoController {
    private final AlocacaoService service;

    private final UsuarioDaSessao sessao;
    public AlocacaoController(AlocacaoService service, UsuarioDaSessao sessao) {
        this.service = service; this.sessao = sessao;
    }

    @PostMapping
    public ResponseEntity<RequisicaoResposta> cadastrar(@Valid @RequestBody NovaRequisicao dados, java.security.Principal principal) {
        if (!sessao.id(principal).equals(dados.usuarioId())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                "O solicitante deve ser o usuário autenticado");
        }
        RequisicaoResposta resposta = service.cadastrar(dados);
        return ResponseEntity.created(URI.create("/api/v1/requisicoes/" + resposta.id())).body(resposta);
    }

    @GetMapping("/{id}")
    public RequisicaoResposta consultar(@PathVariable Long id, java.security.Principal principal) {
        var resposta = service.consultar(id);
        sessao.exigirDono(resposta, principal);
        return resposta;
    }

    @GetMapping("/{id}/proposta-alocacao")
    public PropostaAlocacao propor(@PathVariable Long id, java.security.Principal principal) {
        sessao.exigirDono(service.consultar(id), principal);
        return service.propor(id);
    }

    @PostMapping("/{id}/reserva")
    public RequisicaoResposta confirmar(@PathVariable Long id,
                                        @Valid @RequestBody ConfirmacaoReserva dados, java.security.Principal principal) {
        sessao.exigirDono(service.consultar(id), principal);
        return service.confirmar(id, dados);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratarErro(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(Map.of("erro", erro.getReason()));
    }
}
