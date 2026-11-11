package com.markmadlang.finanzas.seguridad;

import com.jayway.jsonpath.JsonPath;
import com.markmadlang.finanzas.GoogleDePrueba;
import com.markmadlang.finanzas.usuario.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de entrar con Google y de la seguridad de la API.
 * Aquí NO importamos SesionDePrueba: las peticiones van sin pulsera, salvo que se la pongamos a mano.
 * @Transactional deshace todo al acabar cada test (también los usuarios creados).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(GoogleDePrueba.class)
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtEncoder codificador;

    /** Entra con un carné de Google y devuelve la pulsera que da la API. */
    private String entrar(String carne) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credencial\": \"" + carne + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    @Test
    void sinPulseraLaApiDevuelve401() throws Exception {
        mockMvc.perform(get("/api/movimientos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conUnaPulseraInventadaDevuelve401() throws Exception {
        mockMvc.perform(get("/api/movimientos").header("Authorization", "Bearer esto-no-es-una-pulsera"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void entrarConGoogleCreaElUsuarioYDevuelveUnaPulsera() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credencial\": \"" + GoogleDePrueba.CARNE_DE_ANA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.caducaEn").exists())
                .andExpect(jsonPath("$.usuario.email").value("ana@example.com"))
                .andExpect(jsonPath("$.usuario.nombre").value("Ana"))
                .andExpect(jsonPath("$.usuario.foto").value("https://example.com/ana.png"));

        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void conLaPulseraSePuedeUsarLaApi() throws Exception {
        String pulsera = entrar(GoogleDePrueba.CARNE_DE_ANA);

        mockMvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + pulsera))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));

        mockMvc.perform(get("/api/movimientos").header("Authorization", "Bearer " + pulsera))
                .andExpect(status().isOk());
    }

    @Test
    void entrarDosVecesNoDuplicaElUsuarioYActualizaSuNombre() throws Exception {
        entrar(GoogleDePrueba.CARNE_DE_ANA);
        entrar(GoogleDePrueba.CARNE_DE_ANA_CON_NOMBRE_NUEVO); // la misma cuenta de Google, con otro nombre

        assertThat(usuarioRepository.count()).isEqualTo(1);
        assertThat(usuarioRepository.findByGoogleId("google-id-ana").orElseThrow().getNombre()).isEqualTo("Ana García");
    }

    @Test
    void unCarneDeGoogleFalsoDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credencial\": \"carne-inventado\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("No hemos podido comprobar tu cuenta de Google. Vuelve a intentarlo."));

        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void sinCarneDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credencial\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.credencial").value("Falta la credencial de Google"));
    }

    @Test
    void unaPulseraCaducadaDevuelve401() throws Exception {
        Instant haceUnMes = Instant.now().minus(30, ChronoUnit.DAYS);
        String caducada = fabricarPulsera(codificador, haceUnMes, haceUnMes.plus(ServicioTokens.DURACION));

        mockMvc.perform(get("/api/movimientos").header("Authorization", "Bearer " + caducada))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unaPulseraFirmadaConOtraClaveDevuelve401() throws Exception {
        // Alguien intenta fabricarse su propia pulsera, pero no conoce nuestra clave secreta
        var otraClave = new SecretKeySpec("una-clave-que-no-es-la-de-la-api-de-finanzas".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        String falsificada = fabricarPulsera(new NimbusJwtEncoder(new ImmutableSecret<>(otraClave)), Instant.now(), Instant.now().plus(ServicioTokens.DURACION));

        mockMvc.perform(get("/api/movimientos").header("Authorization", "Bearer " + falsificada))
                .andExpect(status().isUnauthorized());
    }

    private static String fabricarPulsera(JwtEncoder codificador, Instant desde, Instant hasta) {
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer(ServicioTokens.EMISOR).subject("1").issuedAt(desde).expiresAt(hasta).build();
        return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), datos)).getTokenValue();
    }
}
