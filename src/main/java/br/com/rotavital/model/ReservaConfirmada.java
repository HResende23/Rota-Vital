package br.com.rotavital.model;

/**
 * Evento de domínio publicado pela alocação quando as bolsas de uma requisição são reservadas.
 * O monitoramento escuta este evento para abrir a remessa na mesma transação da reserva.
 */
public record ReservaConfirmada(Long requisicaoId) {}
