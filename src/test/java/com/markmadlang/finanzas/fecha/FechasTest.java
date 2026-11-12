package com.markmadlang.finanzas.fecha;

import com.markmadlang.finanzas.ConfiguracionReloj;
import com.markmadlang.finanzas.RelojParado;
import com.markmadlang.finanzas.SesionDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las fechas absurdas (año 1, año 10000...) se rechazan con un 400 claro,
 * en lugar de guardarse o de romper la API con un error 500.
 * (Estos fallos los encontró la prueba de caja negra.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({RelojParado.class, SesionDePrueba.class})
class FechasTest {

    @Autowired
    private MockMvc mockMvc;

    private void crear(String url, String json, int estado) throws Exception {
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(estado));
    }

    private static String movimiento(String fecha) {
        return """
                {"tipo": "GASTO", "importe": 5, "fecha": "%s", "descripcion": "Café"}
                """.formatted(fecha);
    }

    @Test
    void movimientoConFechaFueraDeRangoDa400ConMensaje() throws Exception {
        mockMvc.perform(post("/api/movimientos").contentType(MediaType.APPLICATION_JSON).content(movimiento("0001-01-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha").value(Fechas.MENSAJE));
        crear("/api/movimientos", movimiento("1999-12-31"), 400);
        crear("/api/movimientos", movimiento("2101-01-01"), 400);
        crear("/api/movimientos", movimiento("+10000-01-01"), 400);
    }

    @Test
    void losBordesDelRangoSiValen() throws Exception {
        crear("/api/movimientos", movimiento("2000-01-01"), 201);
        crear("/api/movimientos", movimiento("2100-12-31"), 201);
    }

    @Test
    void fijoConInicioFueraDeRangoDa400() throws Exception {
        crear("/api/pagos-recurrentes", """
                {"descripcion": "Netflix", "tipo": "GASTO", "importe": 12.99,
                 "frecuencia": "SEMANAL", "fechaInicio": "0001-01-01"}
                """, 400);
        crear("/api/pagos-recurrentes", """
                {"descripcion": "Netflix", "tipo": "GASTO", "importe": 12.99,
                 "frecuencia": "MENSUAL", "fechaInicio": "9999-12-31"}
                """, 400);
    }

    @Test
    void saldoConFechaFueraDeRangoDa400() throws Exception {
        crear("/api/saldo-banco", "{\"fecha\": \"1999-12-31\", \"importe\": 100}", 400);
    }

    @Test
    void proximosConDesdeExtremoDa400EnVezDeRomperse() throws Exception {
        // Antes: 500 (Java se pasaba de la fecha máxima al sumar los días)
        mockMvc.perform(get("/api/pagos-recurrentes/proximos").param("desde", "+999999999-12-01").param("dias", "366"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.desde").value(Fechas.MENSAJE));
    }

    @Test
    void proximosSinDesdeEmpiezaHoySegunElRelojDeLaApp() throws Exception {
        mockMvc.perform(get("/api/pagos-recurrentes/proximos")).andExpect(status().isOk());
    }

    @Test
    void resumenDeUnMesFueraDeRangoDa400() throws Exception {
        mockMvc.perform(get("/api/resumen").param("mes", "2101-01")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/resumen").param("mes", "1999-12")).andExpect(status().isBadRequest());
    }

    @Test
    void resumenSinMesEsElMesDelRelojDeLaApp() throws Exception {
        mockMvc.perform(get("/api/resumen")).andExpect(jsonPath("$.mes").value("2026-11"));
    }

    @Test
    void elRelojDeLaAppVaEnLaHoraDeEspanaAunqueElServidorEsteEnUtc() {
        assertThat(new ConfiguracionReloj().reloj("Europe/Madrid").getZone()).isEqualTo(ZoneId.of("Europe/Madrid"));
    }
}
