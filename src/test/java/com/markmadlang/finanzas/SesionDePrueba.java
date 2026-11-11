package com.markmadlang.finanzas;

import com.markmadlang.finanzas.seguridad.DatosGoogle;
import com.markmadlang.finanzas.usuario.Usuario;
import com.markmadlang.finanzas.usuario.UsuarioService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Para los tests: crea un usuario de prueba y hace que TODAS las peticiones de MockMvc
 * lleven su pulsera (token), como si ya hubiera iniciado sesión.
 * Así los tests de movimientos, categorías... no tienen que pasar por Google cada vez.
 * Se usa poniendo @Import(SesionDePrueba.class) en la clase de test.
 */
@TestConfiguration
public class SesionDePrueba {

    /**
     * El usuario de los tests, creado como si hubiera entrado con Google.
     * Es el primero de su base de datos de prueba, así que se queda con las 7 categorías de serie.
     */
    @Bean
    public UsuarioDePrueba usuarioDePrueba(UsuarioService usuarioService) {
        Usuario usuario = usuarioService.entrarConGoogle(
                new DatosGoogle("google-id-prueba", "prueba@example.com", "Usuario de prueba", null));
        return new UsuarioDePrueba(usuario.getId());
    }

    @Bean
    public MockMvcBuilderCustomizer peticionesConPulsera(UsuarioDePrueba usuario) {
        return builder -> builder.defaultRequest(
                get("/").with(jwt().jwt(pulsera -> pulsera.subject(String.valueOf(usuario.id())))));
    }

    /** El id del usuario de prueba, para que los tests puedan guardar datos a su nombre. */
    public record UsuarioDePrueba(Long id) {
    }
}
