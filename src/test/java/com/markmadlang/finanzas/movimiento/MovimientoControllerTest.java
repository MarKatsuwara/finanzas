package com.markmadlang.finanzas.movimiento;

import com.markmadlang.finanzas.categoria.Categoria;
import com.markmadlang.finanzas.categoria.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
 * Tests de la API de movimientos. MockMvc simula peticiones HTTP
 * sin arrancar un servidor de verdad, y usa la base de datos H2 en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MovimientoControllerTest {

    private static final String URL = "/api/movimientos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovimientoRepository repository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @BeforeEach
    void limpiarBaseDeDatos() {
        repository.deleteAll(); // cada test empieza con la tabla vacía
    }

    private Movimiento guardar(TipoMovimiento tipo, String importe, String fecha, String descripcion) {
        return repository.save(new Movimiento(tipo, new BigDecimal(importe), LocalDate.parse(fecha), descripcion, null));
    }

    /** Busca una de las categorías que crea la migración V2. */
    private Categoria categoria(String nombre) {
        return categoriaRepository.findByNombreIgnoreCase(nombre).orElseThrow();
    }

    @Test
    void crearConCategoriaDevuelveLaCategoria() throws Exception {
        Long comidaId = categoria("Comida").getId();
        String json = """
                {"tipo": "GASTO", "importe": 20, "fecha": "2026-11-10", "descripcion": "Supermercado", "categoriaId": %d}
                """.formatted(comidaId);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria.id").value(comidaId))
                .andExpect(jsonPath("$.categoria.nombre").value("Comida"));
    }

    @Test
    void crearSinCategoriaDevuelveCategoriaNula() throws Exception {
        String json = """
                {"tipo": "GASTO", "importe": 20, "fecha": "2026-11-10", "descripcion": "Sin clasificar"}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").doesNotExist());
    }

    @Test
    void crearConCategoriaQueNoExisteDevuelve400() throws Exception {
        String json = """
                {"tipo": "GASTO", "importe": 20, "fecha": "2026-11-10", "descripcion": "Algo", "categoriaId": 999999}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.categoriaId").value("No existe ninguna categoría con id 999999"));
    }

    @Test
    void listarIncluyeLaCategoria() throws Exception {
        repository.save(new Movimiento(TipoMovimiento.INGRESO, new BigDecimal("1500.00"),
                LocalDate.parse("2026-11-01"), "Nómina", categoria("Nómina")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria.nombre").value("Nómina"));
    }

    @Test
    void crearMovimientoValidoDevuelve201() throws Exception {
        String json = """
                {"tipo": "GASTO", "importe": 45.90, "fecha": "2026-11-10", "descripcion": "  Compra semanal  "}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tipo").value("GASTO"))
                .andExpect(jsonPath("$.importe").value(45.90))
                .andExpect(jsonPath("$.fecha").value("2026-11-10"))
                .andExpect(jsonPath("$.descripcion").value("Compra semanal")); // sin espacios sobrantes
    }

    @Test
    void crearConDatosVaciosDevuelve400ConTodosLosErrores() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipo").exists())
                .andExpect(jsonPath("$.errores.importe").exists())
                .andExpect(jsonPath("$.errores.fecha").exists())
                .andExpect(jsonPath("$.errores.descripcion").exists());
    }

    @Test
    void crearConImporteNegativoDevuelve400() throws Exception {
        String json = """
                {"tipo": "GASTO", "importe": -5, "fecha": "2026-11-10", "descripcion": "Café"}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.importe").value("El importe debe ser mayor que 0"));
    }

    @Test
    void crearConMasDeDosDecimalesDevuelve400() throws Exception {
        String json = """
                {"tipo": "INGRESO", "importe": 10.555, "fecha": "2026-11-10", "descripcion": "Bizum"}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.importe").exists());
    }

    @Test
    void crearConTipoInventadoDevuelve400() throws Exception {
        String json = """
                {"tipo": "REGALO", "importe": 10, "fecha": "2026-11-10", "descripcion": "Algo"}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarDevuelveLosMasRecientesPrimero() throws Exception {
        guardar(TipoMovimiento.INGRESO, "1500.00", "2026-11-01", "Nómina");
        guardar(TipoMovimiento.GASTO, "3.50", "2026-11-09", "Café");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].descripcion").value("Café"))
                .andExpect(jsonPath("$[1].descripcion").value("Nómina"));
    }

    @Test
    void obtenerPorIdDevuelveElMovimiento() throws Exception {
        Movimiento m = guardar(TipoMovimiento.INGRESO, "1500.00", "2026-11-01", "Nómina");

        mockMvc.perform(get(URL + "/" + m.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Nómina"))
                .andExpect(jsonPath("$.importe").value(1500.00));
    }

    @Test
    void obtenerIdQueNoExisteDevuelve404() throws Exception {
        mockMvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe ningún movimiento con id 999999"));
    }

    @Test
    void actualizarCambiaLosDatos() throws Exception {
        Movimiento m = guardar(TipoMovimiento.GASTO, "50.00", "2026-11-05", "Cena");
        String json = """
                {"tipo": "GASTO", "importe": 62.40, "fecha": "2026-11-06", "descripcion": "Cena con amigos"}
                """;

        mockMvc.perform(put(URL + "/" + m.getId()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importe").value(62.40))
                .andExpect(jsonPath("$.descripcion").value("Cena con amigos"));

        Movimiento guardado = repository.findById(m.getId()).orElseThrow();
        assertThat(guardado.getImporte()).isEqualByComparingTo("62.40");
        assertThat(guardado.getFecha()).isEqualTo(LocalDate.of(2026, 11, 6));
    }

    @Test
    void actualizarIdQueNoExisteDevuelve404() throws Exception {
        String json = """
                {"tipo": "GASTO", "importe": 1, "fecha": "2026-11-06", "descripcion": "X"}
                """;

        mockMvc.perform(put(URL + "/999999").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrarEliminaElMovimiento() throws Exception {
        Movimiento m = guardar(TipoMovimiento.GASTO, "9.99", "2026-11-03", "Netflix");

        mockMvc.perform(delete(URL + "/" + m.getId()))
                .andExpect(status().isNoContent());

        assertThat(repository.existsById(m.getId())).isFalse();
    }

    @Test
    void borrarIdQueNoExisteDevuelve404() throws Exception {
        mockMvc.perform(delete(URL + "/999999"))
                .andExpect(status().isNotFound());
    }
}
