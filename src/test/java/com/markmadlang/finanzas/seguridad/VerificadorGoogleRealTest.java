package com.markmadlang.finanzas.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de las reglas que tiene que cumplir el carné de Google (sin conectarse a Google).
 * La firma no se prueba aquí: de eso se encarga la librería con las claves públicas de Google.
 */
class VerificadorGoogleRealTest {

    private static final Instant AHORA = Instant.parse("2026-11-10T10:00:00Z");
    private static final String NUESTRA_APP = "nuestro-id-de-cliente";

    private final OAuth2TokenValidator<Jwt> reglas =
            VerificadorGoogleReal.validador(NUESTRA_APP, Clock.fixed(AHORA, ZoneOffset.UTC));

    /** Un carné correcto; cada test cambia UNA cosa para ver que se rechaza. */
    private Jwt.Builder carneCorrecto() {
        return Jwt.withTokenValue("carne")
                .header("alg", "RS256")
                .issuer("https://accounts.google.com")
                .audience(List.of(NUESTRA_APP))
                .subject("123")
                .claim("email", "ana@example.com")
                .claim("email_verified", true)
                .issuedAt(AHORA.minusSeconds(60))
                .expiresAt(AHORA.plusSeconds(3600));
    }

    private boolean pasa(Jwt carne) {
        return !reglas.validate(carne).hasErrors();
    }

    @Test
    void unCarneCorrectoPasa() {
        assertThat(pasa(carneCorrecto().build())).isTrue();
    }

    @Test
    void googleTambienPuedeEscribirElEmisorSinHttps() {
        assertThat(pasa(carneCorrecto().issuer("accounts.google.com").build())).isTrue();
    }

    @Test
    void unCarneDeOtraAppNoPasa() {
        assertThat(pasa(carneCorrecto().audience(List.of("la-app-de-otro")).build())).isFalse();
    }

    @Test
    void unCarneQueNoEsDeGoogleNoPasa() {
        assertThat(pasa(carneCorrecto().issuer("https://una-web-cualquiera.com").build())).isFalse();
    }

    @Test
    void unCarneCaducadoNoPasa() {
        assertThat(pasa(carneCorrecto().issuedAt(AHORA.minusSeconds(7200)).expiresAt(AHORA.minusSeconds(3600)).build())).isFalse();
    }

    @Test
    void unEmailSinVerificarNoPasa() {
        assertThat(pasa(carneCorrecto().claim("email_verified", false).build())).isFalse();
    }
}
