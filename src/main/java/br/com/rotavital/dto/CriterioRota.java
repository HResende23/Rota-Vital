package br.com.rotavital.dto;

/** Resultado de um critério de viabilidade da rota (janela, capacidade, autonomia ou validade). */
public record CriterioRota(String nome, boolean atendido, String detalhe) {}
