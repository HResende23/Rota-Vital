package br.com.rotavital.controller;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import br.com.rotavital.dto.*;
import br.com.rotavital.model.Conectividade;
import br.com.rotavital.service.MonitoramentoService;

/** Tela 05: leituras do sensor, alerta térmico e ciclo de transporte da remessa. */
@Controller
public class MonitoramentoTelaController {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final MonitoramentoService monitoramento;
    private final UsuarioDaSessao sessao;

    public MonitoramentoTelaController(MonitoramentoService monitoramento, UsuarioDaSessao sessao) {
        this.monitoramento = monitoramento;
        this.sessao = sessao;
    }

    @GetMapping({"/monitoramento", "/monitoramento.html"})
    public String monitoramento(@RequestParam(required = false) Long remessaId, Model model,
                                Principal principal) {
        List<RemessaResumo> remessas = monitoramento.listarDoUsuario(sessao.id(principal));
        model.addAttribute("remessas", remessas);
        model.addAttribute("conectividades", Conectividade.values());
        Long selecionada = remessaId != null ? remessaId : remessas.stream()
            .filter(r -> r.status().monitorada()).map(RemessaResumo::id).findFirst()
            .orElse(remessas.isEmpty() ? null : remessas.get(0).id());
        if (selecionada != null) {
            RemessaResposta remessa = monitoramento.consultar(selecionada);
            sessao.exigirDono(remessa, principal);
            model.addAttribute("remessa", remessa);
        }
        return "monitoramento";
    }

    @PostMapping("/monitoramento/{id}/leituras")
    public String registrarLeitura(@PathVariable Long id, @Valid @ModelAttribute NovaLeitura leitura,
                                   BindingResult erros, Principal principal, RedirectAttributes flash) {
        exigirDono(id, principal);
        if (erros.hasErrors()) {
            flash.addFlashAttribute("erro", "Informe uma temperatura entre -50 e 60 °C.");
        } else {
            executar(flash, () -> descrever(monitoramento.registrarLeitura(id, leitura)));
        }
        return voltar(id);
    }

    @PostMapping("/monitoramento/{id}/alerta/reconhecimento")
    public String reconhecerAlerta(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        exigirDono(id, principal);
        executar(flash, () -> {
            monitoramento.reconhecerAlerta(id, principal.getName());
            return "Alerta reconhecido. As bolsas continuam bloqueadas até a avaliação técnica.";
        });
        return voltar(id);
    }

    @PostMapping("/monitoramento/{id}/transporte")
    public String iniciarTransporte(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        exigirDono(id, principal);
        executar(flash, () -> "Transporte iniciado: " + monitoramento.iniciarTransporte(id).codigo()
            + " saiu do hemocentro.");
        return voltar(id);
    }

    @PostMapping("/monitoramento/{id}/entrega")
    public String registrarEntrega(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        exigirDono(id, principal);
        executar(flash, () -> "Entrega confirmada: " + monitoramento.registrarEntrega(id).codigo()
            + " foi recebida e o monitoramento foi encerrado.");
        return voltar(id);
    }

    private void exigirDono(Long id, Principal principal) {
        sessao.exigirDono(monitoramento.consultar(id), principal);
    }

    private static void executar(RedirectAttributes flash, Supplier<String> acao) {
        try {
            flash.addFlashAttribute("sucesso", acao.get());
        } catch (ResponseStatusException erro) {
            flash.addFlashAttribute("erro", erro.getReason());
        }
    }

    private static String descrever(RemessaResposta remessa) {
        LeituraResposta leitura = remessa.ultimaLeitura();
        String valor = String.format(PT_BR, "%.1f °C", leitura.temperatura());
        return switch (leitura.condicao()) {
            case DENTRO_DA_FAIXA -> "Leitura de " + valor + " registrada dentro da faixa.";
            case INICIO_DO_DESVIO -> "Leitura de " + valor + " fora da faixa: desvio iniciado. "
                + "Uma nova leitura fora da faixa confirma o alerta térmico.";
            case ALERTA_TERMICO -> "Alerta térmico: " + valor + " confirmou o desvio. "
                + "As bolsas foram bloqueadas e a remessa aguarda avaliação.";
        };
    }

    private static String voltar(Long id) {
        return "redirect:/monitoramento?remessaId=" + id;
    }
}
