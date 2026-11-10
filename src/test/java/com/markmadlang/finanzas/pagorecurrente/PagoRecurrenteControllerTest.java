package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.categoria.CategoriaRepository;
import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la API de pagos recurrentes.
 * @Transactional deshace los cambios al acabar cada test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PagoRecurrenteControllerTest {

    private static final String URL = "/api/pagos-recurrentes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PagoRecurrenteRepository repository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private PagoRecurrente guardar(String descripcion, String importe, Frecuencia frecuencia, boolean activo) {
        return repository.save(new PagoRecurrente(descripcion, TipoMovimiento.GASTO, new BigDecimal(importe),
                frecuencia, LocalDate.of(2026, 1, 1), activo, null));
    }

    @Test
    void crearPagoValidoDevuelve201YQuedaActivo() throws Exception {
        String json = """
                {"descripcion": "  Alquiler  ", "tipo": "GASTO", "importe": 650,
                 "frecuencia": "MENSUAL", "fechaInicio": "2026-01-01"}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.descripcion").value("Alquiler"))
                .andExpect(jsonPath("$.tipo").value("GASTO"))
                .andExpect(jsonPath("$.importe").value(650.00))
                .andExpect(jsonPath("$.frecuencia").value("MENSUAL"))
                .andExpect(jsonPath("$.fechaInicio").value("2026-01-01"))
                .andExpect(jsonPath("$.activo").value(true)) // si no se dice, activo
                .andExpect(jsonPath("$.categoria").doesNotExist());
    }

    @Test
    void crearConCategoriaDevuelveLaCategoria() throws Exception {
        Long id = categoriaRepository.findByNombreIgnoreCase("Suscripciones").orElseThrow().getId();
        String json = """
                {"descripcion": "Netflix", "tipo": "GASTO", "importe": 12.99,
                 "frecuencia": "MENSUAL", "fechaInicio": "2026-03-15", "categoriaId": %d}
                """.formatted(id);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria.nombre").value("Suscripciones"));
    }

    @Test
    void crearConDatosVaciosDevuelve400ConTodosLosErrores() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").exists())
                .andExpect(jsonPath("$.errores.tipo").exists())
                .andExpect(jsonPath("$.errores.importe").exists())
                .andExpect(jsonPath("$.errores.frecuencia").exists())
                .andExpect(jsonPath("$.errores.fechaInicio").exists());
    }

    @Test
    void crearConFrecuenciaInventadaDevuelve400() throws Exception {
        String json = """
                {"descripcion": "Café", "tipo": "GASTO", "importe": 1.5,
                 "frecuencia": "DIARIA", "fechaInicio": "2026-01-01"}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("SEMANAL, MENSUAL o ANUAL")));
    }

    @Test
    void crearConImporteSinDecimalesLoDevuelveConDosDecimales() throws Exception {
        String json = """
                {"descripcion": "Alquiler", "tipo": "GASTO", "importe": 650,
                 "frecuencia": "MENSUAL", "fechaInicio": "2026-01-01"}
                """;

        String respuesta = mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta).contains("\"importe\":650.00");
    }

    @Test
    void crearConCategoriaQueNoExisteDevuelve400() throws Exception {
        String json = """
                {"descripcion": "Gimnasio", "tipo": "GASTO", "importe": 30,
                 "frecuencia": "MENSUAL", "fechaInicio": "2026-01-01", "categoriaId": 999999}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.categoriaId").exists());
    }

    @Test
    void listarDevuelveTodosOrdenadosPorDescripcion() throws Exception {
        guardar("Netflix", "12.99", Frecuencia.MENSUAL, true);
        guardar("Alquiler", "650.00", Frecuencia.MENSUAL, true);
        guardar("Gimnasio", "30.00", Frecuencia.MENSUAL, false);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].descripcion").value("Alquiler"))
                .andExpect(jsonPath("$[1].descripcion").value("Gimnasio"))
                .andExpect(jsonPath("$[1].activo").value(false))
                .andExpect(jsonPath("$[2].descripcion").value("Netflix"));
    }

    @Test
    void obtenerPorIdDevuelveElPago() throws Exception {
        PagoRecurrente p = guardar("Seguro del coche", "420.00", Frecuencia.ANUAL, true);

        mockMvc.perform(get(URL + "/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Seguro del coche"))
                .andExpect(jsonPath("$.frecuencia").value("ANUAL"));
    }

    @Test
    void obtenerIdQueNoExisteDevuelve404() throws Exception {
        mockMvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe ningún pago recurrente con id 999999"));
    }

    @Test
    void actualizarPuedeDesactivarElPago() throws Exception {
        PagoRecurrente p = guardar("Netflix", "12.99", Frecuencia.MENSUAL, true);
        String json = """
                {"descripcion": "Netflix", "tipo": "GASTO", "importe": 13.99,
                 "frecuencia": "MENSUAL", "fechaInicio": "2026-01-01", "activo": false}
                """;

        mockMvc.perform(put(URL + "/" + p.getId()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importe").value(13.99))
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void actualizarSinDecirActivoLoDejaComoEstaba() throws Exception {
        PagoRecurrente p = guardar("Gimnasio", "30.00", Frecuencia.MENSUAL, false);
        String json = """
                {"descripcion": "Gimnasio", "tipo": "GASTO", "importe": 35,
                 "frecuencia": "MENSUAL", "fechaInicio": "2026-01-01"}
                """;

        mockMvc.perform(put(URL + "/" + p.getId()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false)); // sigue desactivado
    }

    @Test
    void actualizarIdQueNoExisteDevuelve404() throws Exception {
        String json = """
                {"descripcion": "X", "tipo": "GASTO", "importe": 1,
                 "frecuencia": "MENSUAL", "fechaInicio": "2026-01-01"}
                """;

        mockMvc.perform(put(URL + "/999999").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrarEliminaElPago() throws Exception {
        PagoRecurrente p = guardar("Netflix", "12.99", Frecuencia.MENSUAL, true);

        mockMvc.perform(delete(URL + "/" + p.getId()))
                .andExpect(status().isNoContent());

        assertThat(repository.existsById(p.getId())).isFalse();
    }

    // ---------- Próximos pagos ----------

    private void guardarConInicio(String descripcion, String importe, Frecuencia frecuencia,
                                  LocalDate inicio, boolean activo) {
        repository.save(new PagoRecurrente(descripcion, TipoMovimiento.GASTO, new BigDecimal(importe),
                frecuencia, inicio, activo, null));
    }

    @Test
    void proximosDevuelveLasFechasOrdenadasYSinLosInactivos() throws Exception {
        guardarConInicio("Alquiler", "650.00", Frecuencia.MENSUAL, LocalDate.of(2026, 1, 1), true);
        guardarConInicio("Netflix", "12.99", Frecuencia.MENSUAL, LocalDate.of(2026, 3, 15), true);
        guardarConInicio("Gimnasio", "30.00", Frecuencia.MENSUAL, LocalDate.of(2026, 1, 20), false); // de baja

        // Del 10/11 al 09/12 (30 días): Netflix el 15/11 y Alquiler el 01/12
        mockMvc.perform(get(URL + "/proximos").param("desde", "2026-11-10").param("dias", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fecha").value("2026-11-15"))
                .andExpect(jsonPath("$[0].descripcion").value("Netflix"))
                .andExpect(jsonPath("$[0].importe").value(12.99))
                .andExpect(jsonPath("$[0].pagoRecurrenteId").isNumber())
                .andExpect(jsonPath("$[1].fecha").value("2026-12-01"))
                .andExpect(jsonPath("$[1].descripcion").value("Alquiler"));
    }

    @Test
    void proximosConDiasCuentaElPrimerDiaYNoSePasa() throws Exception {
        guardarConInicio("Alquiler", "650.00", Frecuencia.MENSUAL, LocalDate.of(2026, 1, 1), true);

        // Todo diciembre: del 01/12 al 31/12. El 01/01 ya no entra.
        mockMvc.perform(get(URL + "/proximos").param("desde", "2026-12-01").param("dias", "31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fecha").value("2026-12-01"));
    }

    @Test
    void proximosSinParametrosUsaHoyYTreintaDias() throws Exception {
        guardarConInicio("Café semanal", "5.00", Frecuencia.SEMANAL, LocalDate.now(), true);

        // Desde hoy, 30 días: hoy, +7, +14, +21 y +28 días
        mockMvc.perform(get(URL + "/proximos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].fecha").value(LocalDate.now().toString()));
    }

    @Test
    void proximosConDiasFueraDeRangoDevuelve400() throws Exception {
        mockMvc.perform(get(URL + "/proximos").param("dias", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.dias").value("Los días deben estar entre 1 y 366"));

        mockMvc.perform(get(URL + "/proximos").param("dias", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void proximosConParametrosMalEscritosDevuelve400() throws Exception {
        mockMvc.perform(get(URL + "/proximos").param("dias", "muchos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.dias").value("El valor 'muchos' no es válido"));

        mockMvc.perform(get(URL + "/proximos").param("desde", "10/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.desde").exists());
    }

    @Test
    void borrarIdQueNoExisteDevuelve404() throws Exception {
        mockMvc.perform(delete(URL + "/999999"))
                .andExpect(status().isNotFound());
    }
}
