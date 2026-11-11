package com.markmadlang.finanzas.seguridad;

import com.jayway.jsonpath.JsonPath;
import com.markmadlang.finanzas.GoogleDePrueba;
import com.markmadlang.finanzas.banco.SaldoBanco;
import com.markmadlang.finanzas.banco.SaldoBancoRepository;
import com.markmadlang.finanzas.movimiento.Movimiento;
import com.markmadlang.finanzas.movimiento.MovimientoRepository;
import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import com.markmadlang.finanzas.pagorecurrente.Frecuencia;
import com.markmadlang.finanzas.pagorecurrente.PagoRecurrente;
import com.markmadlang.finanzas.pagorecurrente.PagoRecurrenteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cada usuario ve SOLO lo suyo. Dos usuarios de prueba, Ana y Luis, entran con Google (de mentira)
 * y comprobamos que ninguno puede ver, cambiar ni borrar nada del otro.
 *
 * @Transactional deshace todo al acabar cada test, así en cada test Ana es siempre la PRIMERA en entrar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(GoogleDePrueba.class)
@Transactional
class SeparacionDeUsuariosTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private PagoRecurrenteRepository pagoRecurrenteRepository;

    @Autowired
    private SaldoBancoRepository saldoBancoRepository;

    // ---------- Ayudantes ----------

    /** Entra con un carné de Google y devuelve la pulsera. */
    private String entrar(String carne) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credencial\": \"" + carne + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    /** Le pone la pulsera a una petición: "esta petición la hace este usuario". */
    private static MockHttpServletRequestBuilder como(String pulsera, MockHttpServletRequestBuilder peticion) {
        return peticion.header("Authorization", "Bearer " + pulsera);
    }

    /** Crea algo (POST) con la pulsera de un usuario y devuelve su id. */
    private long crear(String pulsera, String url, String json) throws Exception {
        String respuesta = mockMvc.perform(como(pulsera, post(url)).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private static String movimiento(String descripcion, String importe, Long categoriaId) {
        return """
                {"tipo": "GASTO", "importe": %s, "fecha": "2026-11-05", "descripcion": "%s", "categoriaId": %s}
                """.formatted(importe, descripcion, categoriaId);
    }

    private static final String NETFLIX = """
            {"descripcion": "Netflix", "tipo": "GASTO", "importe": 12.99, "frecuencia": "MENSUAL", "fechaInicio": "2026-11-15"}
            """;

    // ---------- Tests ----------

    @Test
    void elPrimeroEnEntrarSeQuedaConLosDatosSinDuenoYElSegundoNo() throws Exception {
        // Datos de "antes de que hubiera usuarios" (sin dueño), como los que ya tenías apuntados
        movimientoRepository.save(new Movimiento(null, TipoMovimiento.GASTO, new BigDecimal("25.00"),
                LocalDate.parse("2026-11-05"), "Cena de antes", null));
        pagoRecurrenteRepository.save(new PagoRecurrente(null, "Alquiler", TipoMovimiento.GASTO, new BigDecimal("650.00"),
                Frecuencia.MENSUAL, LocalDate.parse("2026-11-01"), true, null));
        saldoBancoRepository.save(new SaldoBanco(null, LocalDate.parse("2026-11-01"), new BigDecimal("400.00")));

        String ana = entrar(GoogleDePrueba.CARNE_DE_ANA); // la primera
        mockMvc.perform(como(ana, get("/api/movimientos")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].descripcion").value("Cena de antes"));
        mockMvc.perform(como(ana, get("/api/pagos-recurrentes"))).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(como(ana, get("/api/saldo-banco/historial"))).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(como(ana, get("/api/categorias"))).andExpect(jsonPath("$", hasSize(7))); // las de serie

        String luis = entrar(GoogleDePrueba.CARNE_DE_LUIS); // el segundo: no hereda nada
        mockMvc.perform(como(luis, get("/api/movimientos"))).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(como(luis, get("/api/pagos-recurrentes"))).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(como(luis, get("/api/saldo-banco/historial"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void cadaUsuarioNuevoTieneSusPropiasCategoriasDeSerie() throws Exception {
        String ana = entrar(GoogleDePrueba.CARNE_DE_ANA);
        String luis = entrar(GoogleDePrueba.CARNE_DE_LUIS);

        List<Integer> deAna = JsonPath.read(mockMvc.perform(como(ana, get("/api/categorias")))
                .andReturn().getResponse().getContentAsString(), "$[*].id");
        List<Integer> deLuis = JsonPath.read(mockMvc.perform(como(luis, get("/api/categorias")))
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].nombre").value("Comida"))
                .andReturn().getResponse().getContentAsString(), "$[*].id");

        assertThat(deLuis).doesNotContainAnyElementsOf(deAna); // son SUS categorías, no las de Ana
    }

    @Test
    void luisNoPuedeVerCambiarNiBorrarLosMovimientosDeAna() throws Exception {
        String ana = entrar(GoogleDePrueba.CARNE_DE_ANA);
        String luis = entrar(GoogleDePrueba.CARNE_DE_LUIS);
        long id = crear(ana, "/api/movimientos", movimiento("Cena", "30", null));

        mockMvc.perform(como(luis, get("/api/movimientos"))).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(como(luis, get("/api/movimientos/" + id))).andExpect(status().isNotFound());
        mockMvc.perform(como(luis, put("/api/movimientos/" + id))
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("Hackeado", "1", null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(como(luis, delete("/api/movimientos/" + id))).andExpect(status().isNotFound());

        // Y a Ana no le ha pasado nada
        mockMvc.perform(como(ana, get("/api/movimientos/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Cena"));
    }

    @Test
    void luisNoPuedeVerCambiarNiBorrarLosGastosFijosDeAna() throws Exception {
        String ana = entrar(GoogleDePrueba.CARNE_DE_ANA);
        String luis = entrar(GoogleDePrueba.CARNE_DE_LUIS);
        long id = crear(ana, "/api/pagos-recurrentes", NETFLIX);

        mockMvc.perform(como(luis, get("/api/pagos-recurrentes"))).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(como(luis, get("/api/pagos-recurrentes/proximos?desde=2026-11-01&dias=60")))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(como(luis, get("/api/pagos-recurrentes/" + id))).andExpect(status().isNotFound());
        mockMvc.perform(como(luis, put("/api/pagos-recurrentes/" + id)).contentType(MediaType.APPLICATION_JSON).content(NETFLIX))
                .andExpect(status().isNotFound());
        mockMvc.perform(como(luis, delete("/api/pagos-recurrentes/" + id))).andExpect(status().isNotFound());

        mockMvc.perform(como(ana, get("/api/pagos-recurrentes/" + id))).andExpect(status().isOk());
    }

    @Test
    void lasCategoriasSonDeCadaUno() throws Exception {
        String ana = entrar(GoogleDePrueba.CARNE_DE_ANA);
        String luis = entrar(GoogleDePrueba.CARNE_DE_LUIS);
        long gimnasioDeAna = crear(ana, "/api/categorias", "{\"nombre\": \"Gimnasio\"}");

        // Luis no la ve...
        mockMvc.perform(como(luis, get("/api/categorias/" + gimnasioDeAna))).andExpect(status().isNotFound());
        mockMvc.perform(como(luis, delete("/api/categorias/" + gimnasioDeAna))).andExpect(status().isNotFound());

        // ...ni puede usarla en sus movimientos
        mockMvc.perform(como(luis, post("/api/movimientos"))
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("Cuota", "30", gimnasioDeAna)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.categoriaId").value("No existe ninguna categoría con id " + gimnasioDeAna));

        // Y puede tener su propia "Gimnasio" sin chocar con la de Ana
        crear(luis, "/api/categorias", "{\"nombre\": \"Gimnasio\"}");
    }

    @Test
    void borrarMiCuentaBorraTodoLoMioYMiPulseraDejaDeValer() throws Exception {
        String ana = entrar(GoogleDePrueba.CARNE_DE_ANA);
        String luis = entrar(GoogleDePrueba.CARNE_DE_LUIS);
        long gimnasio = crear(ana, "/api/categorias", "{\"nombre\": \"Gimnasio\"}");
        crear(ana, "/api/movimientos", movimiento("Cuota", "30", gimnasio)); // un movimiento que usa su categoría
        crear(ana, "/api/pagos-recurrentes", NETFLIX);
        crear(ana, "/api/saldo-banco", "{\"fecha\": \"2026-11-01\", \"importe\": 400}");
        long cenaDeLuis = crear(luis, "/api/movimientos", movimiento("Cena", "20", null));

        mockMvc.perform(como(ana, delete("/api/auth/yo"))).andExpect(status().isNoContent());

        // De Ana no queda nada...
        assertThat(movimientoRepository.findByUsuarioIdOrderByFechaDescIdDesc(idDe(ana))).isEmpty();
        assertThat(pagoRecurrenteRepository.findByUsuarioIdOrderByDescripcionAscIdAsc(idDe(ana))).isEmpty();
        assertThat(saldoBancoRepository.findByUsuarioIdOrderByFechaDescIdDesc(idDe(ana))).isEmpty();
        // ...y su pulsera ya no sirve (la web la mandará a la pantalla de entrar)
        mockMvc.perform(como(ana, get("/api/movimientos"))).andExpect(status().isUnauthorized());

        // A Luis no le ha pasado nada
        mockMvc.perform(como(luis, get("/api/movimientos/" + cenaDeLuis))).andExpect(status().isOk());
    }

    /** El id de usuario que va dentro de una pulsera (su campo "sub"). */
    private static Long idDe(String pulsera) {
        String datos = new String(java.util.Base64.getUrlDecoder().decode(pulsera.split("\\.")[1]));
        return Long.valueOf(JsonPath.<String>read(datos, "$.sub"));
    }

    @Test
    void elResumenYElSaldoDelBancoSonDeCadaUno() throws Exception {
        String ana = entrar(GoogleDePrueba.CARNE_DE_ANA);
        String luis = entrar(GoogleDePrueba.CARNE_DE_LUIS);
        crear(ana, "/api/movimientos", movimiento("Cena", "50", null));
        crear(ana, "/api/saldo-banco", "{\"fecha\": \"2026-11-01\", \"importe\": 400}");

        mockMvc.perform(como(ana, get("/api/resumen?mes=2026-11"))).andExpect(jsonPath("$.totalGastos").value(50.00));
        mockMvc.perform(como(luis, get("/api/resumen?mes=2026-11"))).andExpect(jsonPath("$.totalGastos").value(0.00));

        mockMvc.perform(como(ana, get("/api/saldo-banco"))).andExpect(status().isOk());
        mockMvc.perform(como(luis, get("/api/saldo-banco"))).andExpect(status().isNotFound()); // Luis no ha apuntado nada
    }
}
