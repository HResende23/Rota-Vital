package br.com.rotavital.controller;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import br.com.rotavital.dto.*;
import br.com.rotavital.service.MonitoramentoService;
import br.com.rotavital.service.RotaService;

/** API REST de remessas: monitoramento térmico (leituras e alerta), planejamento de rota e entrega. */
@RestController
@RequestMapping("/api/v1/remessas")
public class RemessaController {
    private final MonitoramentoService monitoramento;
    private final RotaService rotas;
    private final UsuarioDaSessao sessao;

    public RemessaController(MonitoramentoService monitoramento, RotaService rotas, UsuarioDaSessao sessao) {
        this.monitoramento = monitoramento;
        this.rotas = rotas;
        this.sessao = sessao;
    }

    @GetMapping
    public List<RemessaResumo> listar(Principal principal) {
        return monitoramento.listarDoUsuario(sessao.id(principal));
    }

    @GetMapping("/{id}")
    public RemessaResposta consultar(@PathVariable Long id, Principal principal) {
        return doDono(id, principal);
    }

    /** Leitura enviada pelo sensor da caixa térmica. */
    @PostMapping("/{id}/leituras")
    public ResponseEntity<RemessaResposta> registrarLeitura(@PathVariable Long id,
            @Valid @RequestBody NovaLeitura leitura, Principal principal) {
        doDono(id, principal);
        RemessaResposta remessa = monitoramento.registrarLeitura(id, leitura);
        return ResponseEntity.created(URI.create("/api/v1/remessas/" + id)).body(remessa);
    }

    @PostMapping("/{id}/alerta/reconhecimento")
    public RemessaResposta reconhecerAlerta(@PathVariable Long id, Principal principal) {
        doDono(id, principal);
        return monitoramento.reconhecerAlerta(id, principal.getName());
    }

    @GetMapping("/{id}/rota/alternativas")
    public PlanejamentoRota simularRota(@PathVariable Long id, @RequestParam String destino,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime saida,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime janelaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime janelaFim,
            Principal principal) {
        doDono(id, principal);
        return rotas.simular(id, new ParametrosRota(destino, saida, janelaInicio, janelaFim));
    }

    @PostMapping("/{id}/rota")
    public RemessaResposta confirmarRota(@PathVariable Long id, @Valid @RequestBody ConfirmacaoRota dados,
                                         Principal principal) {
        doDono(id, principal);
        return rotas.confirmar(id, dados.parametros(), dados.caminho());
    }

    @PostMapping("/{id}/transporte")
    public RemessaResposta iniciarTransporte(@PathVariable Long id, Principal principal) {
        doDono(id, principal);
        return monitoramento.iniciarTransporte(id);
    }

    @PostMapping("/{id}/entrega")
    public RemessaResposta registrarEntrega(@PathVariable Long id, Principal principal) {
        doDono(id, principal);
        return monitoramento.registrarEntrega(id);
    }

    private RemessaResposta doDono(Long id, Principal principal) {
        RemessaResposta remessa = monitoramento.consultar(id);
        sessao.exigirDono(remessa, principal);
        return remessa;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratarErro(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(Map.of("erro", erro.getReason()));
    }
}
