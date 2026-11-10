package com.markmadlang.finanzas;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * El "reloj" de la app. En vez de llamar a LocalDate.now() directamente,
 * las clases que necesitan saber qué día es piden este Clock.
 *
 * ¿Para qué? En los tests podemos cambiarlo por un reloj PARADO en una fecha fija,
 * y así los tests dan siempre el mismo resultado, se ejecuten el día que se ejecuten.
 */
@Configuration
public class ConfiguracionReloj {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone(); // la fecha y hora reales del ordenador
    }
}
