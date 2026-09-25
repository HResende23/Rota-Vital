package br.com.rotavital.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.rotavital.model.BolsaHemoComponente;
import br.com.rotavital.model.StatusBolsa;
import br.com.rotavital.model.TipoComponente;
import br.com.rotavital.model.TipoSanguineo;

@Service
public class EstoqueService {

    private final List<BolsaHemoComponente> bolsas = new ArrayList<>();

    public EstoqueService() {

        bolsas.add(
            new BolsaHemoComponente(
                "B101",
                TipoComponente.CONCENTRADO_HEMACIAS,
                TipoSanguineo.A_POSITIVO,
                LocalDate.of(2026, 10, 20),
                StatusBolsa.DISPONIVEL
            )
        );

        bolsas.add(
            new BolsaHemoComponente(
                "B102",
                TipoComponente.PLASMA,
                TipoSanguineo.O_NEGATIVO,
                LocalDate.of(2026, 11, 15),
                StatusBolsa.DISPONIVEL
            )
        );
    }

    public List<BolsaHemoComponente> listarTodas() {
        return bolsas;
    }
}