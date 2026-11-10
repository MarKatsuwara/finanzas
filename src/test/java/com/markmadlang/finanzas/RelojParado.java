package com.markmadlang.finanzas;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Para los tests: sustituye el reloj real por uno PARADO en el 10 de noviembre de 2026.
 * Se usa poniendo @Import(RelojParado.class) en la clase de test.
 */
@TestConfiguration
public class RelojParado {

    @Bean
    @Primary // "usa este en vez del normal"
    public Clock relojParado() {
        return Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneId.of("Europe/Madrid"));
    }
}
