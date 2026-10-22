package br.com.rotavital.controller;

import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

@ControllerAdvice
public class SessaoModel {
    @ModelAttribute
    public void sessao(Principal principal, Model model) {
        model.addAttribute("autenticado", principal != null);
        if (principal != null) model.addAttribute("nomeUsuario", principal.getName());
    }
}
