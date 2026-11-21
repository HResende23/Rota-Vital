package br.com.rotavital.dto;

public record AnalisePearson(
    double r,
    double r2,
    String intensidade,
    String direcao,
    String interpretacao
) {
}