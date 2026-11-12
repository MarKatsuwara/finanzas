package com.markmadlang.finanzas.pagorecurrente;

import com.jayway.jsonpath.JsonPath;
import com.markmadlang.finanzas.RelojParado;
import com.markmadlang.finanzas.SesionDePrueba;
import com.markmadlang.finanzas.categoria.Categoria;
import com.markmadlang.finanzas.categoria.CategoriaRepository;
import com.markmadlang.finanzas.movimiento.MovimientoRepository;
import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import com.markmadlang.finanzas.seguridad.DatosGoogle;
import com.markmadlang.finanzas.usuario.Usuario;
import com.markmadlang.finanzas.usuario.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de los fijos que se apuntan SOLOS como movimientos.
 * Para la app, "hoy" es el 10/11/2026 (RelojParado).
 *
 * Truco: un fijo guardado directamente en el repositorio empieza con la marca el día ANTES de su primer pago
 * ("nada apuntado todavía"). Así, un fijo mensual que empezó el 01/10 tiene pendientes el 01/10 y el 01/11.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({RelojParado.class, SesionDePrueba.class})
class ApuntadorDeFijosTest {

    private static final String URL = "/api/pagos-recurrentes/apuntar-pendientes";
    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SesionDePrueba.UsuarioDePrueba usuario;

    @Autowired
    private PagoRecurrenteRepository pagoRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UsuarioService usuarioService;

    private PagoRecurrente guardar(Long dueno, String descripcion, TipoMovimiento tipo, String importe,
                                   Frecuencia frecuencia, LocalDate inicio, Categoria categoria) {
        return pagoRepository.save(new PagoRecurrente(dueno, descripcion, tipo, new BigDecimal(importe),
                frecuencia, inicio, true, categoria));
    }

    private PagoRecurrente guardar(String descripcion, String importe, Frecuencia frecuencia, LocalDate inicio) {
        return guardar(usuario.id(), descripcion, TipoMovimiento.GASTO, importe, frecuencia, inicio, null);
    }

    @Test
    void apuntaLoQueTocabaDesdeLaMarcaHastaHoyConSusDatos() throws Exception {
        Categoria vivienda = categoriaRepository.findByUsuarioIdAndNombreIgnoreCase(usuario.id(), "Vivienda").orElseThrow();
        PagoRecurrente alquiler = guardar(usuario.id(), "Alquiler", TipoMovimiento.GASTO, "650",
                Frecuencia.MENSUAL, LocalDate.of(2026, 10, 1), vivienda);
        guardar(usuario.id(), "Nómina", TipoMovimiento.INGRESO, "1500", Frecuencia.MENSUAL, LocalDate.of(2026, 11, 1), null);

        mockMvc.perform(post(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                // Ordenados por fecha, y el mismo día por nombre
                .andExpect(jsonPath("$[0].fecha").value("2026-10-01"))
                .andExpect(jsonPath("$[0].descripcion").value("Alquiler"))
                .andExpect(jsonPath("$[0].tipo").value("GASTO"))
                .andExpect(jsonPath("$[0].importe").value(650.00))
                .andExpect(jsonPath("$[0].categoria.nombre").value("Vivienda"))
                .andExpect(jsonPath("$[0].automatico").value(true))
                .andExpect(jsonPath("$[1].fecha").value("2026-11-01"))
                .andExpect(jsonPath("$[1].descripcion").value("Alquiler"))
                .andExpect(jsonPath("$[2].fecha").value("2026-11-01"))
                .andExpect(jsonPath("$[2].descripcion").value("Nómina"))
                .andExpect(jsonPath("$[2].tipo").value("INGRESO"));

        // Quedan guardados como movimientos de verdad, enlazados a su fijo, y la marca pasa a HOY
        assertThat(movimientoRepository.findByUsuarioIdOrderByFechaDescIdDesc(usuario.id()))
                .hasSize(3)
                .allSatisfy(m -> assertThat(m.isAutomatico()).isTrue());
        assertThat(movimientoRepository.findByUsuarioIdOrderByFechaDescIdDesc(usuario.id()).getLast().getPagoRecurrenteId())
                .isEqualTo(alquiler.getId());
        assertThat(alquiler.getApuntadoHasta()).isEqualTo(HOY);

        // Y en la lista de movimientos salen marcados como automáticos
        mockMvc.perform(get("/api/movimientos"))
                .andExpect(jsonPath("$[0].automatico").value(true));
    }

    @Test
    void laSegundaVezNoApuntaNadaPorqueNadaSeApuntaDosVeces() throws Exception {
        guardar("Netflix", "12.99", Frecuencia.MENSUAL, LocalDate.of(2026, 10, 15));

        mockMvc.perform(post(URL)).andExpect(jsonPath("$", hasSize(1))); // el 15/10 (el 15/11 aún no ha llegado)
        mockMvc.perform(post(URL)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        assertThat(movimientoRepository.countByUsuarioId(usuario.id())).isEqualTo(1);
    }

    @Test
    void loDeHoySeApuntaPeroLoDeMananaNo() throws Exception {
        guardar("Gimnasio", "32", Frecuencia.MENSUAL, HOY);                 // toca hoy
        guardar("Spotify", "10.99", Frecuencia.MENSUAL, HOY.plusDays(1));   // toca mañana

        mockMvc.perform(post(URL))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].descripcion").value("Gimnasio"))
                .andExpect(jsonPath("$[0].fecha").value("2026-11-10"));
    }

    @Test
    void losApagadosYLosQueTienenApuntarSoloApagadoNoSeApuntan() throws Exception {
        PagoRecurrente apagado = guardar("Revista", "5", Frecuencia.MENSUAL, LocalDate.of(2026, 10, 1));
        apagado.actualizar("Revista", TipoMovimiento.GASTO, new BigDecimal("5"), Frecuencia.MENSUAL,
                LocalDate.of(2026, 10, 1), false, null);
        PagoRecurrente luz = guardar("Luz", "60", Frecuencia.MENSUAL, LocalDate.of(2026, 10, 5));
        luz.setApuntarSolo(false); // cambia cada mes: se apunta a mano

        mockMvc.perform(post(URL)).andExpect(jsonPath("$", hasSize(0)));
        assertThat(movimientoRepository.countByUsuarioId(usuario.id())).isZero();
    }

    @Test
    void crearUnFijoPorLaApiPoneLaMarcaEnHoyYNoRellenaElPasado() throws Exception {
        // Empezó en enero, pero lo creas hoy: no se apuntan los 10 meses de atrás (ni el de hoy)
        String json = """
                {"descripcion": "Alquiler", "tipo": "GASTO", "importe": 650,
                 "frecuencia": "MENSUAL", "fechaInicio": "2026-01-10"}
                """;
        mockMvc.perform(post("/api/pagos-recurrentes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apuntarSolo").value(true)); // de serie, encendido

        mockMvc.perform(post(URL)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sePuedeCrearConApuntarSoloApagadoYCambiarloAlEditar() throws Exception {
        String json = """
                {"descripcion": "Luz", "tipo": "GASTO", "importe": 60,
                 "frecuencia": "MENSUAL", "fechaInicio": "2026-01-05", "apuntarSolo": false}
                """;
        String respuesta = mockMvc.perform(post("/api/pagos-recurrentes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(jsonPath("$.apuntarSolo").value(false))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(respuesta, "$.id");

        // Editar sin decir nada de "apuntarSolo": se queda como estaba
        mockMvc.perform(put("/api/pagos-recurrentes/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"descripcion": "Luz", "tipo": "GASTO", "importe": 65,
                         "frecuencia": "MENSUAL", "fechaInicio": "2026-01-05"}
                        """))
                .andExpect(jsonPath("$.apuntarSolo").value(false));
    }

    @Test
    void alEncenderApuntarSoloNoSeRellenaLoQuePasoMientrasEstabaApagado() throws Exception {
        PagoRecurrente luz = guardar("Luz", "60", Frecuencia.MENSUAL, LocalDate.of(2026, 9, 5));
        luz.setApuntarSolo(false);

        mockMvc.perform(put("/api/pagos-recurrentes/" + luz.getId()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"descripcion": "Luz", "tipo": "GASTO", "importe": 60,
                         "frecuencia": "MENSUAL", "fechaInicio": "2026-09-05", "apuntarSolo": true}
                        """))
                .andExpect(jsonPath("$.apuntarSolo").value(true));

        // El 05/09, 05/10 y 05/11 pasaron con el interruptor apagado: no se apuntan
        mockMvc.perform(post(URL)).andExpect(jsonPath("$", hasSize(0)));
        assertThat(luz.getApuntadoHasta()).isEqualTo(HOY);
    }

    @Test
    void alVolverAEncenderUnFijoApagadoTampocoSeRellena() throws Exception {
        PagoRecurrente revista = guardar("Revista", "5", Frecuencia.MENSUAL, LocalDate.of(2026, 9, 1));
        revista.actualizar("Revista", TipoMovimiento.GASTO, new BigDecimal("5"), Frecuencia.MENSUAL,
                LocalDate.of(2026, 9, 1), false, null);

        mockMvc.perform(put("/api/pagos-recurrentes/" + revista.getId()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"descripcion": "Revista", "tipo": "GASTO", "importe": 5,
                         "frecuencia": "MENSUAL", "fechaInicio": "2026-09-01", "activo": true}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(post(URL)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void soloApuntaLosFijosDelUsuarioQueAbreLaApp() throws Exception {
        Usuario ana = usuarioService.entrarConGoogle(new DatosGoogle("google-ana", "ana@example.com", "Ana", null));
        PagoRecurrente deAna = guardar(ana.getId(), "Gimnasio de Ana", TipoMovimiento.GASTO, "40",
                Frecuencia.MENSUAL, LocalDate.of(2026, 10, 1), null);

        mockMvc.perform(post(URL)).andExpect(jsonPath("$", hasSize(0)));

        assertThat(movimientoRepository.countByUsuarioId(ana.getId())).isZero();
        assertThat(deAna.getApuntadoHasta()).isEqualTo(LocalDate.of(2026, 9, 30)); // su marca, sin tocar
    }

    @Test
    void siBorrasElFijoSusMovimientosSeQuedan() throws Exception {
        PagoRecurrente netflix = guardar("Netflix", "12.99", Frecuencia.MENSUAL, LocalDate.of(2026, 10, 15));
        mockMvc.perform(post(URL)).andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/pagos-recurrentes/" + netflix.getId())).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/movimientos"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].descripcion").value("Netflix"))
                .andExpect(jsonPath("$[0].automatico").value(true));
    }

    @Test
    void losMovimientosApuntadosAManoNoSonAutomaticos() throws Exception {
        mockMvc.perform(post("/api/movimientos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipo": "GASTO", "importe": 5, "fecha": "2026-11-01", "descripcion": "Café"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.automatico").value(false));
    }
}
