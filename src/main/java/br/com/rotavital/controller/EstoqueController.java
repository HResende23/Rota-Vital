package br.com.rotavital.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.rotavital.model.BolsaHemoComponente;
import br.com.rotavital.service.EstoqueService;

@RestController
@RequestMapping("/api/v1/bolsas")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping
    public List<BolsaHemoComponente> listarTodas() {
        return estoqueService.listarTodas();
    }

    @PostMapping
    public ResponseEntity<BolsaHemoComponente> cadastrar(
            @RequestBody BolsaHemoComponente bolsa) {

        BolsaHemoComponente novaBolsa =
                estoqueService.cadastrar(bolsa);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(novaBolsa);
    }
}