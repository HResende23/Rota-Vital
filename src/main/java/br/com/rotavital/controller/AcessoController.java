package br.com.rotavital.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import br.com.rotavital.dto.RegistroForm;
import br.com.rotavital.service.UserService;

@Controller
public class AcessoController {
    private final UserService users;
    public AcessoController(UserService users) { this.users = users; }

    @GetMapping("/")
    public String inicio(java.security.Principal principal) {
        return principal == null ? "redirect:/login" : "redirect:/estoque";
    }

    @GetMapping({"/login", "/login.html"})
    public String login(java.security.Principal principal) {
        return principal == null ? "login" : "redirect:/estoque";
    }

    @GetMapping({"/registro", "/registro.html"})
    public String registro(java.security.Principal principal) {
        return principal == null ? "registro" : "redirect:/estoque";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute RegistroForm dados,
                            BindingResult erros, Model model) {
        // Nunca devolve a senha ao HTML ou ao flash da sessão.
        model.addAttribute("nome", dados.nome());
        model.addAttribute("email", dados.email());
        if (erros.hasErrors()) {
            model.addAttribute("erro", "Confira nome, e-mail e senha (8 a 72 caracteres/até 72 bytes). As senhas devem ser iguais.");
            return "registro";
        }
        try {
            users.registrar(dados);
            return "redirect:/login?cadastrado";
        } catch (ResponseStatusException erro) {
            model.addAttribute("erro", erro.getReason());
            return "registro";
        }
    }
}
