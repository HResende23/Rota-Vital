package br.com.rotavital.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio usado nas leituras, no planejamento de rotas e nas mudanças de situação da remessa.
 * Centralizar o horário facilita testes; no Docker o fuso vem da variável TZ do Compose.
 */
@Configuration
public class RelogioConfig {
    @Bean
    Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
