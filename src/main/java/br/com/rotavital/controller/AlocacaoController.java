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

    public AlocacaoController(AlocacaoService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<RequisicaoResposta> cadastrar(@Valid @RequestBody NovaRequisicao dados) {
        RequisicaoResposta resposta = service.cadastrar(dados);
        return ResponseEntity.created(URI.create("/api/v1/requisicoes/" + resposta.id())).body(resposta);
    }

    @GetMapping("/{id}")
    public RequisicaoResposta consultar(@PathVariable Long id) { return service.consultar(id); }

    @GetMapping("/{id}/proposta-alocacao")
    public PropostaAlocacao propor(@PathVariable Long id) { return service.propor(id); }

    @PostMapping("/{id}/reserva")
    public RequisicaoResposta confirmar(@PathVariable Long id,
                                        @Valid @RequestBody ConfirmacaoReserva dados) {
        return service.confirmar(id, dados);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratarErro(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(Map.of("erro", erro.getReason()));
    }
}
