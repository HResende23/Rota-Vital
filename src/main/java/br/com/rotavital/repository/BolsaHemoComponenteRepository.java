package br.com.rotavital.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.rotavital.model.BolsaHemoComponente;

public interface BolsaHemoComponenteRepository
        extends JpaRepository<BolsaHemoComponente, String> {
}