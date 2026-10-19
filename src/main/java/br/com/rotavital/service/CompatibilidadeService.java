package br.com.rotavital.service;

import org.springframework.stereotype.Service;
import br.com.rotavital.model.*;

/** Regras exclusivamente didáticas do protótipo, sem validação clínica. */
@Service
public class CompatibilidadeService {
    public boolean compativel(TipoComponente componente, TipoSanguineo doador,
                              TipoSanguineo receptor) {
        // Nesta entrega, a matriz ampliada abrange apenas concentrado de hemácias.
        // Para plasma e plaquetas, o escopo acadêmico aceita somente tipo idêntico.
        if (componente != TipoComponente.CONCENTRADO_HEMACIAS) {
            return doador == receptor;
        }
        String aboDoador = doador.name().split("_")[0];
        String aboReceptor = receptor.name().split("_")[0];
        boolean aboCompativel = aboDoador.equals("O") || aboDoador.equals(aboReceptor)
            || aboReceptor.equals("AB");
        boolean rhCompativel = doador.name().endsWith("NEGATIVO")
            || receptor.name().endsWith("POSITIVO");
        return aboCompativel && rhCompativel;
    }
}
