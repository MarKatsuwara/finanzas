package com.markmadlang.finanzas.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Set;

/**
 * El verificador de verdad: comprueba el carné de Google.
 *
 * El carné es un JWT firmado por Google. Comprobamos 4 cosas:
 *  1. La FIRMA es de Google: descargamos las claves públicas de Google y miramos que encajen.
 *     (Nadie puede falsificar la firma sin la clave secreta, que solo tiene Google.)
 *  2. Lo ha emitido Google (campo "iss").
 *  3. Es para NUESTRA app (campo "aud" = nuestro ID de cliente). Así nadie puede usar
 *     el carné que le dio otra web.
 *  4. No está caducado, y el email está verificado por Google.
 */
@Component
public class VerificadorGoogleReal implements VerificadorGoogle {

    /** Donde Google publica sus claves públicas para comprobar firmas. */
    private static final String CLAVES_DE_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";

    private static final Set<String> EMISORES_DE_GOOGLE = Set.of("https://accounts.google.com", "accounts.google.com");

    private final String idCliente;
    private final NimbusJwtDecoder decodificador;

    public VerificadorGoogleReal(@Value("${finanzas.google.id-cliente:}") String idCliente, Clock reloj) {
        this.idCliente = idCliente;
        this.decodificador = NimbusJwtDecoder.withJwkSetUri(CLAVES_DE_GOOGLE).build();
        this.decodificador.setJwtValidator(validador(idCliente, reloj));
    }

    @Override
    public DatosGoogle verificar(String credencial) {
        if (idCliente.isBlank()) {
            throw new GoogleSinConfigurarException();
        }

        Jwt carne;
        try {
            carne = decodificador.decode(credencial); // comprueba la firma y las reglas de validador()
        } catch (JwtException e) {
            throw new CredencialNoValidaException();
        }

        String email = carne.getClaimAsString("email");
        String nombre = carne.getClaimAsString("name");
        return new DatosGoogle(
                carne.getSubject(),
                email,
                nombre == null || nombre.isBlank() ? email : nombre, // si no tiene nombre, usamos el email
                carne.getClaimAsString("picture"));
    }

    /** Las reglas que tiene que cumplir el carné (aparte de la firma). Es "static" para poder probarlas en un test. */
    static OAuth2TokenValidator<Jwt> validador(String idCliente, Clock reloj) {
        JwtTimestampValidator noCaducado = new JwtTimestampValidator();
        noCaducado.setClock(reloj);

        OAuth2TokenValidator<Jwt> deGoogle = carne -> regla(
                EMISORES_DE_GOOGLE.contains(carne.getClaimAsString("iss")), "El carné no lo ha emitido Google");

        OAuth2TokenValidator<Jwt> paraNuestraApp = carne -> regla(
                carne.getAudience() != null && carne.getAudience().contains(idCliente), "El carné es para otra app");

        OAuth2TokenValidator<Jwt> emailVerificado = carne -> regla(
                Boolean.TRUE.equals(carne.getClaimAsBoolean("email_verified")), "El email no está verificado");

        return new DelegatingOAuth2TokenValidator<>(noCaducado, deGoogle, paraNuestraApp, emailVerificado);
    }

    private static OAuth2TokenValidatorResult regla(boolean cumple, String motivo) {
        return cumple
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", motivo, null));
    }
}
