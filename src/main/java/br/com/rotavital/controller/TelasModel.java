package br.com.rotavital.controller;

import java.security.Principal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import br.com.rotavital.model.TipoComponente;
import br.com.rotavital.model.TipoSanguineo;
import br.com.rotavital.service.MalhaViaria;
import br.com.rotavital.service.UserService;

/** Dados comuns às telas operacionais: usuário da barra lateral, opções dos formulários e hospitais da malha. */
@ControllerAdvice(assignableTypes = {TelasController.class, RotaTelaController.class,
    MonitoramentoTelaController.class})
public class TelasModel {
    private final UserService users;
    private final MalhaViaria malha;

    public TelasModel(UserService users, MalhaViaria malha) {
        this.users = users;
        this.malha = malha;
    }

    @ModelAttribute
    public void comuns(Model model, Principal principal) {
        if (principal != null) model.addAttribute("usuario", users.atual(principal.getName()));
        model.addAttribute("componentes", TipoComponente.values());
        model.addAttribute("tipos", TipoSanguineo.values());
        model.addAttribute("hospitais", malha.hospitais());
    }
}
