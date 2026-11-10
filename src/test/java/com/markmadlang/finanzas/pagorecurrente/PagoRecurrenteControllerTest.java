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

    @Test
    void borrarIdQueNoExisteDevuelve404() throws Exception {
        mockMvc.perform(delete(URL + "/999999"))
                .andExpect(status().isNotFound());
    }
}
