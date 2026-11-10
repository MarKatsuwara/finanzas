package com.markmadlang.finanzas.categoria;

import com.markmadlang.finanzas.movimiento.Movimiento;
import com.markmadlang.finanzas.movimiento.MovimientoRepository;
import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import com.markmadlang.finanzas.pagorecurrente.Frecuencia;
import com.markmadlang.finanzas.pagorecurrente.PagoRecurrente;
import com.markmadlang.finanzas.pagorecurrente.PagoRecurrenteRepository;
import org.junit.jupiter.api.BeforeEach;
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
 * Tests de la API de categorías.
 * @Transactional deshace todos los cambios al acabar cada test,
 * así las 7 categorías de serie siempre están igual al empezar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoriaControllerTest {

    private static final String URL = "/api/categorias";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaRepository repository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private PagoRecurrenteRepository pagoRecurrenteRepository;

    @BeforeEach
    void limpiarMovimientos() {
        movimientoRepository.deleteAll(); // que ningún movimiento de otros tests use las categorías
    }

    private Long idDe(String nombre) {
        return repository.findByNombreIgnoreCase(nombre).orElseThrow().getId();
    }

    private String json(String nombre) {
        return "{\"nombre\": \"" + nombre + "\"}";
    }

    @Test
    void listarDevuelveLasCategoriasDeSerieOrdenadas() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].nombre").value("Comida"))
                .andExpect(jsonPath("$[6].nombre").value("Vivienda"));
    }

    @Test
    void obtenerPorIdDevuelveLaCategoria() throws Exception {
        mockMvc.perform(get(URL + "/" + idDe("Ocio")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ocio"));
    }

    @Test
    void obtenerIdQueNoExisteDevuelve404() throws Exception {
        mockMvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe ninguna categoría con id 999999"));
    }

    @Test
    void crearCategoriaNuevaDevuelve201() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json("  Salud  ")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Salud")); // sin espacios sobrantes
    }

    @Test
    void crearConNombreRepetidoDevuelve409() throws Exception {
        // "comida" en minúsculas también cuenta como repetido
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json("comida")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe una categoría llamada comida"));
    }

    @Test
    void crearSinNombreDevuelve400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio"));
    }

    @Test
    void renombrarCambiaElNombre() throws Exception {
        Long id = idDe("Ocio");

        mockMvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON).content(json("Ocio y caprichos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ocio y caprichos"));
    }

    @Test
    void renombrarConSuMismoNombreFunciona() throws Exception {
        // Cambiar solo mayúsculas/minúsculas de la propia categoría no es un conflicto
        mockMvc.perform(put(URL + "/" + idDe("Ocio")).contentType(MediaType.APPLICATION_JSON).content(json("OCIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("OCIO"));
    }

    @Test
    void renombrarAUnNombreDeOtraDevuelve409() throws Exception {
        mockMvc.perform(put(URL + "/" + idDe("Ocio")).contentType(MediaType.APPLICATION_JSON).content(json("Comida")))
                .andExpect(status().isConflict());
    }

    @Test
    void renombrarIdQueNoExisteDevuelve404() throws Exception {
        mockMvc.perform(put(URL + "/999999").contentType(MediaType.APPLICATION_JSON).content(json("Lo que sea")))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrarCategoriaSinUsarDevuelve204() throws Exception {
        Long id = idDe("Otros");

        mockMvc.perform(delete(URL + "/" + id))
                .andExpect(status().isNoContent());

        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void borrarCategoriaEnUsoDevuelve409() throws Exception {
        Categoria comida = repository.findByNombreIgnoreCase("Comida").orElseThrow();
        movimientoRepository.save(new Movimiento(TipoMovimiento.GASTO, new BigDecimal("10.00"),
                LocalDate.of(2026, 11, 10), "Pan", comida));

        mockMvc.perform(delete(URL + "/" + comida.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("No se puede borrar la categoría Comida porque hay movimientos que la usan"));

        assertThat(repository.existsById(comida.getId())).isTrue();
    }

    @Test
    void borrarCategoriaUsadaPorUnPagoRecurrenteDevuelve409() throws Exception {
        Categoria suscripciones = repository.findByNombreIgnoreCase("Suscripciones").orElseThrow();
        pagoRecurrenteRepository.save(new PagoRecurrente("Netflix", TipoMovimiento.GASTO, new BigDecimal("12.99"),
                Frecuencia.MENSUAL, LocalDate.of(2026, 1, 1), true, suscripciones));

        mockMvc.perform(delete(URL + "/" + suscripciones.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("No se puede borrar la categoría Suscripciones porque hay pagos recurrentes que la usan"));
    }

    @Test
    void borrarIdQueNoExisteDevuelve404() throws Exception {
        mockMvc.perform(delete(URL + "/999999"))
                .andExpect(status().isNotFound());
    }
}
