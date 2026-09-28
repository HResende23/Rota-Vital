package br.com.rotavital.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.rotavital.model.BolsaHemoComponente;
import br.com.rotavital.repository.BolsaHemoComponenteRepository;

@Service
public class EstoqueService {

    private final BolsaHemoComponenteRepository repository;

    public EstoqueService(BolsaHemoComponenteRepository repository) {
        this.repository = repository;
    }

    public List<BolsaHemoComponente> listarTodas() {
        return repository.findAll();
    }

    public BolsaHemoComponente cadastrar(BolsaHemoComponente bolsa) {

        if (repository.existsById(bolsa.getCodigo())) {
            throw new IllegalArgumentException(
                "Já existe uma bolsa cadastrada com o código " + bolsa.getCodigo()
            );
        }

        return repository.save(bolsa);
    }
}