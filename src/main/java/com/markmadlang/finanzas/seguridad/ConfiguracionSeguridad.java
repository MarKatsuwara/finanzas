package com.markmadlang.finanzas.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * Las reglas de seguridad de la API. Es el "portero de la discoteca":
 *  - para entrar con Google (POST /api/auth/google) no hace falta pulsera;
 *  - para todo lo demás de /api, sí. Sin pulsera válida → 401 (no autorizado).
 */
@Configuration
public class ConfiguracionSeguridad {

    @Bean
    public SecurityFilterChain reglas(HttpSecurity http) throws Exception {
        http
                // CSRF protege a las webs que usan cookies. Nosotros usamos tokens en una cabecera, no cookies.
                .csrf(csrf -> csrf.disable())
                // La API no "recuerda" a nadie entre peticiones: cada petición trae su pulsera
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(peticiones -> peticiones
                        .requestMatchers(HttpMethod.POST, "/api/auth/google").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                // Lee la cabecera "Authorization: Bearer <token>" y comprueba el token con jwtDecoder()
                .oauth2ResourceServer(servidor -> servidor.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /** La clave secreta con la que firmamos las pulseras. Sale de la variable de entorno JWT_SECRET. */
    @Bean
    public SecretKey claveDeLasPulseras(@Value("${finanzas.jwt.secreto:}") String secreto) {
        if (secreto.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno JWT_SECRET (la clave para firmar las sesiones)");
        }
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET es demasiado corta: necesita al menos 32 caracteres");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** Fabrica pulseras (lo usa ServicioTokens). */
    @Bean
    public JwtEncoder codificadorDePulseras(SecretKey clave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    /** Comprueba pulseras en cada petición: firma correcta, emitida por nosotros y sin caducar. */
    @Bean
    public JwtDecoder lectorDePulseras(SecretKey clave, Clock reloj) {
        NimbusJwtDecoder lector = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();

        JwtTimestampValidator noCaducada = new JwtTimestampValidator();
        noCaducada.setClock(reloj); // usa el mismo reloj que el resto de la app (en los tests, el parado)
        lector.setJwtValidator(new DelegatingOAuth2TokenValidator<>(noCaducada, new JwtIssuerValidator(ServicioTokens.EMISOR)));
        return lector;
    }
}
