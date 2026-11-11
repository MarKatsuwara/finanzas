package com.markmadlang.finanzas.seguridad;

import com.markmadlang.finanzas.usuario.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Fabrica las "pulseras" (tokens JWT) de NUESTRA app.
 *
 * Un JWT es un texto con 3 partes separadas por puntos: cabecera.datos.firma
 *  - en los datos va quién eres (tu id de usuario) y hasta cuándo vale;
 *  - la firma se hace con una clave secreta que solo conoce la API (JWT_SECRET).
 * Si alguien cambia una sola letra de los datos, la firma deja de encajar y la API lo rechaza.
 */
@Service
public class ServicioTokens {

    /** Quién emite las pulseras: lo comprobamos al leerlas, para no aceptar tokens de otros sitios. */
    public static final String EMISOR = "finanzas";

    /** Cuánto dura una sesión. Pasado ese tiempo, hay que volver a entrar con Google. */
    public static final Duration DURACION = Duration.ofDays(7);

    private final JwtEncoder codificador;
    private final Clock reloj;

    public ServicioTokens(JwtEncoder codificador, Clock reloj) {
        this.codificador = codificador;
        this.reloj = reloj;
    }

    public Token crear(Usuario usuario) {
        Instant ahora = reloj.instant();
        Instant caducaEn = ahora.plus(DURACION);

        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(String.valueOf(usuario.getId())) // "sub": de quién es la pulsera
                .issuedAt(ahora)
                .expiresAt(caducaEn)
                .claim("email", usuario.getEmail())
                .build();

        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build(); // HS256: firma con clave secreta
        String valor = codificador.encode(JwtEncoderParameters.from(cabecera, datos)).getTokenValue();
        return new Token(valor, caducaEn);
    }

    /** Una pulsera ya fabricada: el texto del token y hasta cuándo vale. */
    public record Token(String valor, Instant caducaEn) {
    }
}
