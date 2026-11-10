package br.com.rotavital.service;

import java.text.Normalizer;
import java.util.*;
import org.springframework.stereotype.Component;
import br.com.rotavital.model.*;
import static br.com.rotavital.model.PosicaoRotulo.*;
import static br.com.rotavital.model.TipoPonto.*;
import static br.com.rotavital.model.TipoVia.*;

/**
 * Malha viária sintética usada no planejamento: o hemocentro, pontos de apoio e os hospitais
 * atendidos, ligados por trechos com duração em trânsito livre. Os dados são didáticos.
 */
@Component
public class MalhaViaria {
    public static final String HEMOCENTRO = "HEMOCENTRO";

    private final Map<String, PontoDaMalha> pontos = new LinkedHashMap<>();
    private final List<Trecho> trechos;

    public MalhaViaria() {
        ponto(HEMOCENTRO, "Hemocentro Central", TipoPonto.HEMOCENTRO, 210, 300, ESQUERDA);
        ponto("P1", "Anel Viário (P1)", APOIO, 330, 140, ESQUERDA);
        ponto("P2", "Corredor Norte (P2)", APOIO, 590, 80, ACIMA);
        ponto("P3", "Centro (P3)", APOIO, 330, 440, ABAIXO);
        ponto("P4", "Via Sul (P4)", APOIO, 600, 520, ABAIXO);
        ponto("AURORA", "Hospital Aurora", HOSPITAL, 650, 300, DIREITA);
        ponto("VALE_VERDE", "Hospital Vale Verde", HOSPITAL, 890, 60, ABAIXO);
        ponto("PRIMAVERA", "Hospital Primavera", HOSPITAL, 890, 210, ABAIXO);
        ponto("HORIZONTE", "Hospital Horizonte", HOSPITAL, 890, 390, ACIMA);
        ponto("BOM_PASTOR", "Hospital Bom Pastor", HOSPITAL, 890, 530, ACIMA);
        trechos = List.of(
            new Trecho(HEMOCENTRO, "AURORA", 40, URBANA),
            new Trecho(HEMOCENTRO, "P1", 18, EXPRESSA),
            new Trecho(HEMOCENTRO, "P3", 14, URBANA),
            new Trecho(HEMOCENTRO, "P4", 34, EXPRESSA),
            new Trecho("P1", "AURORA", 32, EXPRESSA),
            new Trecho("P1", "P2", 16, EXPRESSA),
            new Trecho("P1", "VALE_VERDE", 44, URBANA),
            new Trecho("P1", "PRIMAVERA", 46, URBANA),
            new Trecho("P2", "VALE_VERDE", 20, URBANA),
            new Trecho("P2", "PRIMAVERA", 24, EXPRESSA),
            new Trecho("P3", "AURORA", 30, URBANA),
            new Trecho("P3", "P4", 18, URBANA),
            new Trecho("P3", "HORIZONTE", 38, URBANA),
            new Trecho("P4", "HORIZONTE", 22, URBANA),
            new Trecho("P4", "BOM_PASTOR", 16, URBANA));
        trechos.forEach(t -> { exigir(t.origem()); exigir(t.destino()); });
    }

    private void ponto(String codigo, String nome, TipoPonto tipo, int x, int y, PosicaoRotulo rotulo) {
        pontos.put(codigo, new PontoDaMalha(codigo, nome, tipo, x, y, rotulo));
    }

    public PontoDaMalha hemocentro() { return exigir(HEMOCENTRO); }
    public List<PontoDaMalha> getPontos() { return List.copyOf(pontos.values()); }
    public List<Trecho> getTrechos() { return trechos; }

    public List<PontoDaMalha> hospitais() {
        return pontos.values().stream().filter(PontoDaMalha::isHospital).toList();
    }

    public Optional<PontoDaMalha> ponto(String codigo) {
        return Optional.ofNullable(codigo == null ? null : pontos.get(codigo));
    }

    /** Ponto obrigatório (usado no desenho do mapa); falha se o código não pertence à malha. */
    public PontoDaMalha pontoDe(String codigo) {
        return exigir(codigo);
    }

    /**
     * Relaciona o hospital digitado na requisição a um hospital da malha, ignorando
     * maiúsculas, acentos e o prefixo "Hospital" (por exemplo, "aurora" → Hospital Aurora).
     */
    public Optional<PontoDaMalha> hospitalCorrespondente(String nomeInformado) {
        if (nomeInformado == null || nomeInformado.isBlank()) {
            return Optional.empty();
        }
        String procurado = normalizar(nomeInformado);
        return hospitais().stream().filter(h -> {
            String nome = normalizar(h.nome());
            return nome.equals(procurado) || nome.equals("hospital " + procurado);
        }).findFirst();
    }

    /**
     * Caminhos simples do ponto de origem ao destino, com no máximo {@code maximoDeTrechos}.
     * Outros hospitais não são usados como passagem, para que cada caminho seja uma entrega única.
     */
    public List<Caminho> caminhos(String origem, String destino, int maximoDeTrechos) {
        exigir(origem);
        exigir(destino);
        List<Caminho> encontrados = new ArrayList<>();
        List<String> visitados = new ArrayList<>(List.of(origem));
        buscar(origem, destino, maximoDeTrechos, visitados, new ArrayList<>(), encontrados);
        return encontrados;
    }

    private void buscar(String atual, String destino, int restantes, List<String> visitados,
                        List<Trecho> percorridos, List<Caminho> encontrados) {
        if (atual.equals(destino)) {
            encontrados.add(new Caminho(visitados.stream().map(this::exigir).toList(), percorridos));
            return;
        }
        if (restantes == 0 || (visitados.size() > 1 && exigir(atual).isHospital())) {
            return;
        }
        for (Trecho trecho : trechos) {
            if (!trecho.liga(atual)) {
                continue;
            }
            String proximo = trecho.outraPonta(atual);
            if (visitados.contains(proximo)) {
                continue;
            }
            visitados.add(proximo);
            percorridos.add(trecho);
            buscar(proximo, destino, restantes - 1, visitados, percorridos, encontrados);
            visitados.remove(visitados.size() - 1);
            percorridos.remove(percorridos.size() - 1);
        }
    }

    private PontoDaMalha exigir(String codigo) {
        PontoDaMalha ponto = pontos.get(codigo);
        if (ponto == null) {
            throw new IllegalArgumentException("Ponto fora da malha: " + codigo);
        }
        return ponto;
    }

    static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
    }
}
