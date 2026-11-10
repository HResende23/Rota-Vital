package br.com.rotavital.dto;

import java.time.LocalDateTime;

/** Passo da linha do tempo da remessa. Situação: concluida, atual, pendente ou bloqueada. */
public record EtapaRemessa(String nome, LocalDateTime em, String situacao) {}
