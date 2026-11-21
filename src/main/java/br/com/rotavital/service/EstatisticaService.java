package br.com.rotavital.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.rotavital.dto.AnalisePearson;
import br.com.rotavital.dto.LeituraResposta;

@Service
public class EstatisticaService {

    public AnalisePearson calcularPearson(List<LeituraResposta> leituras) {

        if (leituras == null || leituras.size() < 2) {
            return null;
        }

        LocalDateTime inicio = leituras.get(0).registradaEm();

        int n = leituras.size();

        double somaX = 0;
        double somaY = 0;

        for (LeituraResposta leitura : leituras) {
            double x = Duration.between(inicio, leitura.registradaEm()).toSeconds();
            double y = leitura.temperatura();

            somaX += x;
            somaY += y;
        }

        double mediaX = somaX / n;
        double mediaY = somaY / n;

        double somaProduto = 0;
        double somaQuadradoX = 0;
        double somaQuadradoY = 0;

        for (LeituraResposta leitura : leituras) {

            double x = Duration.between(inicio, leitura.registradaEm()).toSeconds();
            double y = leitura.temperatura();

            double desvioX = x - mediaX;
            double desvioY = y - mediaY;

            somaProduto += desvioX * desvioY;
            somaQuadradoX += desvioX * desvioX;
            somaQuadradoY += desvioY * desvioY;
        }

        double denominador =
            Math.sqrt(somaQuadradoX * somaQuadradoY);

        if (denominador == 0) {
            return null;
        }

        double r = somaProduto / denominador;
        double r2 = r * r;

        String direcao;

        if (r > 0) {
            direcao = "positiva";
        } else if (r < 0) {
            direcao = "negativa";
        } else {
            direcao = "sem tendência linear";
        }

        double absoluto = Math.abs(r);

        String intensidade;

        if (absoluto < 0.20) {
            intensidade = "muito fraca";
        } else if (absoluto < 0.40) {
            intensidade = "fraca";
        } else if (absoluto < 0.70) {
            intensidade = "moderada";
        } else if (absoluto < 0.90) {
            intensidade = "forte";
        } else {
            intensidade = "muito forte";
        }

        String interpretacao;

        if (r > 0) {
            interpretacao =
                "A temperatura apresenta tendência de aumento ao longo do tempo.";
        } else if (r < 0) {
            interpretacao =
                "A temperatura apresenta tendência de queda ao longo do tempo.";
        } else {
            interpretacao =
                "Não foi identificada tendência linear da temperatura ao longo do tempo.";
        }

        return new AnalisePearson(
            r,
            r2,
            intensidade,
            direcao,
            interpretacao
        );
    }
}