package com.markmadlang.finanzas;

import com.markmadlang.finanzas.seguridad.CredencialNoValidaException;
import com.markmadlang.finanzas.seguridad.DatosGoogle;
import com.markmadlang.finanzas.seguridad.VerificadorGoogle;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Para los tests: un "Google de mentira". Los tests no pueden iniciar sesión en Google de verdad,
 * así que este verificador conoce unos pocos carnés inventados y rechaza todo lo demás.
 */
@TestConfiguration
public class GoogleDePrueba {

    public static final String CARNE_DE_ANA = "carne-google-de-ana";
    public static final String CARNE_DE_ANA_CON_NOMBRE_NUEVO = "carne-google-de-ana-nombre-nuevo";
    public static final String CARNE_DE_LUIS = "carne-google-de-luis";

    @Bean
    @Primary // "usa este en vez del de verdad"
    public VerificadorGoogle googleDeMentira() {
        return credencial -> switch (credencial) {
            case CARNE_DE_ANA -> new DatosGoogle("google-id-ana", "ana@example.com", "Ana", "https://example.com/ana.png");
            case CARNE_DE_ANA_CON_NOMBRE_NUEVO -> new DatosGoogle("google-id-ana", "ana@example.com", "Ana García", null);
            case CARNE_DE_LUIS -> new DatosGoogle("google-id-luis", "luis@example.com", "Luis", null);
            default -> throw new CredencialNoValidaException();
        };
    }
}
