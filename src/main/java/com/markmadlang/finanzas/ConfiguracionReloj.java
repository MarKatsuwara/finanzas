package com.markmadlang.finanzas;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * El "reloj" de la app. En vez de llamar a LocalDate.now() directamente,
 * las clases que necesitan saber qué día es piden este Clock.
 *
 * ¿Para qué? En los tests podemos cambiarlo por un reloj PARADO en una fecha fija,
 * y así los tests dan siempre el mismo resultado, se ejecuten el día que se ejecuten.
 */
@Configuration
public class ConfiguracionReloj {

    /**
     * La fecha y hora reales, pero SIEMPRE en la hora de España (o la de finanzas.zona-horaria).
     *
     * Antes se usaba la zona del ordenador. En tu PC da igual, pero el servidor de Render está en hora UTC
     * (1 o 2 horas menos): entre las 00:00 y la 01:00 de España, la API creía que todavía era AYER
     * (el saldo de hoy, la previsión y los fijos que se apuntan solos usaban el día equivocado).
     */
    @Bean
    public Clock reloj(@Value("${finanzas.zona-horaria:Europe/Madrid}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
