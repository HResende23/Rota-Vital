package br.com.rotavital.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import br.com.rotavital.dto.*;
import br.com.rotavital.service.MalhaViaria;
import br.com.rotavital.service.MonitoramentoService;
import br.com.rotavital.service.RotaService;

/** Tela 04: compara caminhos da malha sintética e confirma o planejamento da remessa. */
@Controller
public class RotaTelaController {
    private final MonitoramentoService monitoramento;
    private final RotaService rotas;
    private final MalhaViaria malha;
    private final UsuarioDaSessao sessao;

    public RotaTelaController(MonitoramentoService monitoramento, RotaService rotas,
                              MalhaViaria malha, UsuarioDaSessao sessao) {
        this.monitoramento = monitoramento;
        this.rotas = rotas;
        this.malha = malha;
        this.sessao = sessao;
    }

    @GetMapping({"/rota", "/rota.html"})
    public String rota(@RequestParam(required = false) Long remessaId,
                       @RequestParam(required = false) String destino,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime saida,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime janelaInicio,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime janelaFim,
                       Model model, Principal principal) {
        List<RemessaResumo> remessas = monitoramento.listarDoUsuario(sessao.id(principal));
        model.addAttribute("remessas", remessas);
        model.addAttribute("malha", malha);
        Long selecionada = remessaId != null ? remessaId : remessas.stream()
            .filter(r -> r.status().permitePlanejamento()).map(RemessaResumo::id).findFirst().orElse(null);
        if (selecionada == null) {
            return "rota";
        }
        RemessaResposta remessa = monitoramento.consultar(selecionada);
        sessao.exigirDono(remessa, principal);
        model.addAttribute("remessa", remessa);
        if (!remessa.isPermitePlanejamento()) {
            return "rota";
        }
        ParametrosRota parametros = rotas.sugerir(remessa, destino, saida, janelaInicio, janelaFim);
        model.addAttribute("parametros", parametros);
        if (parametros.destino() == null) {
            model.addAttribute("aviso", "O hospital \"" + remessa.hospital()
                + "\" não foi reconhecido na malha. Escolha o destino correspondente para simular.");
            return "rota";
        }
        try {
            model.addAttribute("planejamento", rotas.simular(selecionada, parametros));
        } catch (ResponseStatusException erro) {
            model.addAttribute("erro", erro.getReason());
        }
        return "rota";
    }

    @PostMapping("/rota/{remessaId}/planejamento")
    public String confirmar(@PathVariable Long remessaId, @Valid @ModelAttribute ParametrosRota parametros,
                            BindingResult erros, @RequestParam(required = false) String caminho,
                            Principal principal, RedirectAttributes flash) {
        sessao.exigirDono(monitoramento.consultar(remessaId), principal);
        if (erros.hasErrors()) {
            flash.addFlashAttribute("erro", "Informe o hospital de destino, a saída e a janela de recebimento.");
            return "redirect:/rota?remessaId=" + remessaId;
        }
        try {
            RemessaResposta remessa = rotas.confirmar(remessaId, parametros, caminho);
            flash.addFlashAttribute("sucesso", "Rota de " + remessa.codigo() + " planejada: "
                + remessa.plano().getCaminhoDescricao() + ". O monitoramento segue ativo até a entrega.");
            return "redirect:/monitoramento?remessaId=" + remessaId;
        } catch (ResponseStatusException erro) {
            flash.addFlashAttribute("erro", erro.getReason());
            return "redirect:" + UriComponentsBuilder.fromPath("/rota")
                .queryParam("remessaId", remessaId)
                .queryParam("destino", parametros.destino())
                .queryParam("saida", parametros.saida())
                .queryParam("janelaInicio", parametros.janelaInicio())
                .queryParam("janelaFim", parametros.janelaFim())
                .encode().toUriString();
        }
    }
}
