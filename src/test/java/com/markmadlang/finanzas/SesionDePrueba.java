package com.markmadlang.finanzas;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Para los tests: hace que TODAS las peticiones de MockMvc lleven una pulsera (token) válida,
 * como si ya hubieras iniciado sesión. Así los tests de movimientos, categorías...
 * no tienen que pasar por Google cada vez.
 * Se usa poniendo @Import(SesionDePrueba.class) en la clase de test.
 */
@TestConfiguration
public class SesionDePrueba {

    @Bean
    public MockMvcBuilderCustomizer peticionesConPulsera() {
        return builder -> builder.defaultRequest(get("/").with(jwt().jwt(pulsera -> pulsera.subject("1"))));
    }
}
