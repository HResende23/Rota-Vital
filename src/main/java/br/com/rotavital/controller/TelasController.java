package br.com.rotavital.controller;

import java.security.Principal;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import br.com.rotavital.dto.*;
import br.com.rotavital.model.*;
import br.com.rotavital.service.*;

@Controller
public class TelasController {
    private final UserService users;
    private final EstoqueService estoque;
    private final AlocacaoService alocacao;
    private final UsuarioDaSessao sessao;

    public TelasController(UserService users, EstoqueService estoque,
                           AlocacaoService alocacao, UsuarioDaSessao sessao) {
        this.users = users; this.estoque = estoque; this.alocacao = alocacao; this.sessao = sessao;
    }

    @ModelAttribute
    public void comuns(Model model, Principal principal) {
        if (principal != null) model.addAttribute("usuario", users.atual(principal.getName()));
        model.addAttribute("componentes", TipoComponente.values());
        model.addAttribute("tipos", TipoSanguineo.values());
    }

    @GetMapping({"/estoque", "/index.html"})
    public String estoque(Model model, @RequestParam(required = false) TipoComponente componente,
                           @RequestParam(required = false) TipoSanguineo tipo) {
        var todas = estoque.listarTodas();
        model.addAttribute("bolsas", todas.stream()
            .filter(b -> componente == null || b.getComponente() == componente)
            .filter(b -> tipo == null || b.getTipoSanguineo() == tipo)
            .sorted(java.util.Comparator.comparing(BolsaHemoComponente::getValidade)
                .thenComparing(BolsaHemoComponente::getCodigo)).toList());
        model.addAttribute("disponiveis", todas.stream().filter(BolsaHemoComponente::estaDisponivel).count());
        model.addAttribute("reservadas", todas.stream().filter(b -> b.getStatus() == StatusBolsa.RESERVADA).count());
        model.addAttribute("bloqueadas", todas.stream().filter(b -> b.getStatus() == StatusBolsa.INDISPONIVEL || b.estaVencida()).count());
        model.addAttribute("filtroComponente", componente);
        model.addAttribute("filtroTipo", tipo);
        return "index";
    }

    @PostMapping("/bolsas")
    public String cadastrarBolsa(@Valid @ModelAttribute NovaBolsaForm dados, BindingResult erros,
                                 RedirectAttributes flash) {
        if (erros.hasErrors()) {
            flash.addFlashAttribute("erro", "Preencha os dados da bolsa e informe uma validade a partir de hoje.");
        } else {
            try {
                estoque.cadastrar(new BolsaHemoComponente(dados.codigo().strip(), dados.componente(),
                    dados.tipoSanguineo(), dados.validade(), StatusBolsa.DISPONIVEL));
                flash.addFlashAttribute("sucesso", "Bolsa cadastrada com sucesso.");
            } catch (IllegalArgumentException erro) {
                flash.addFlashAttribute("erro", erro.getMessage());
            }
        }
        return "redirect:/estoque";
    }

    @GetMapping({"/requisicao", "/requisicao.html"})
    public String requisicao(Model model, Principal principal) {
        model.addAttribute("requisicoes", alocacao.listarDoUsuario(sessao.id(principal)));
        return "requisicao";
    }

    @PostMapping("/requisicoes")
    public String enviar(@Valid @ModelAttribute RequisicaoForm dados, BindingResult erros,
                          Principal principal, Model model, RedirectAttributes flash) {
        if (erros.hasErrors()) {
            model.addAttribute("erro", "Preencha todos os campos. A quantidade deve estar entre 1 e 1000.");
            model.addAttribute("dados", dados);
            return requisicao(model, principal);
        }
        var resposta = alocacao.cadastrar(new NovaRequisicao(sessao.id(principal), dados.hospital(),
            dados.componente(), dados.tipoSanguineo(), dados.quantidade()));
        flash.addFlashAttribute("sucesso", "Requisição criada com sucesso.");
        return "redirect:/alocacao?requisicaoId=" + resposta.id();
    }

    @GetMapping({"/alocacao", "/alocacao.html"})
    public String alocacao(@RequestParam(required = false) Long requisicaoId,
                            Model model, Principal principal) {
        model.addAttribute("requisicoes", alocacao.listarDoUsuario(sessao.id(principal)));
        if (requisicaoId != null) {
            var requisicao = alocacao.consultar(requisicaoId);
            sessao.exigirDono(requisicao, principal);
            model.addAttribute("requisicao", requisicao);
            if (requisicao.status() == StatusRequisicao.PENDENTE_ALOCACAO) {
                model.addAttribute("proposta", alocacao.propor(requisicaoId));
            }
        }
        return "alocacao";
    }

    @PostMapping("/alocacao/{id}/reserva")
    public String reservar(@PathVariable Long id,
                            @RequestParam(required = false) List<String> codigosBolsas,
                            Principal principal, RedirectAttributes flash) {
        sessao.exigirDono(alocacao.consultar(id), principal);
        try {
            if (codigosBolsas == null || codigosBolsas.isEmpty()) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Selecione as bolsas.");
            }
            alocacao.confirmar(id, new ConfirmacaoReserva(codigosBolsas));
            flash.addFlashAttribute("sucesso", "Reserva confirmada. As bolsas foram reservadas no estoque.");
        } catch (ResponseStatusException erro) {
            flash.addFlashAttribute("erro", erro.getReason());
        }
        return "redirect:/alocacao?requisicaoId=" + id;
    }

    @GetMapping({"/rota", "/rota.html"})
    public String rota() { return "rota"; }

    @GetMapping({"/monitoramento", "/monitoramento.html"})
    public String monitoramento() { return "monitoramento"; }
}
