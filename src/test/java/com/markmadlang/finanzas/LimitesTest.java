package com.markmadlang.finanzas;

import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import com.markmadlang.finanzas.pagorecurrente.Frecuencia;
import com.markmadlang.finanzas.pagorecurrente.PagoRecurrente;
import com.markmadlang.finanzas.pagorecurrente.PagoRecurrenteRepository;
import com.markmadlang.finanzas.seguridad.DatosGoogle;
import com.markmadlang.finanzas.usuario.Usuario;
import com.markmadlang.finanzas.usuario.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de los límites por usuario.
 * En la app de verdad son 20.000 movimientos, 100 categorías...; aquí los ponemos MUY pequeños
 * para no tener que crear miles de cosas en cada test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SesionDePrueba.class)
@Transactional
@TestPropertySource(properties = {
        "finanzas.limites.movimientos=2",
        "finanzas.limites.categorias=8", // el usuario de prueba ya tiene las 7 de serie: le cabe 1 más
        "finanzas.limites.gastos-fijos=1",
        "finanzas.limites.saldos=1"
})
class LimitesTest {

    private static final String MOVIMIENTO = """
            {"tipo": "GASTO", "importe": 5, "fecha": "2026-11-01", "descripcion": "Café"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PagoRecurrenteRepository pagoRecurrenteRepository;

    private MvcResult crear(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();
    }

    @Test
    void alLlegarAlMaximoDeMovimientosNoDejaApuntarMasHastaQueBorresUno() throws Exception {
        crear("/api/movimientos", MOVIMIENTO);
        String ubicacion = crear("/api/movimientos", MOVIMIENTO).getResponse().getHeader("Location");

        // El tercero ya no cabe: 409 y un mensaje que la web enseña tal cual
        mockMvc.perform(post("/api/movimientos").contentType(MediaType.APPLICATION_JSON).content(MOVIMIENTO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Has llegado al máximo de 2 movimientos. Borra alguno para poder guardar más."));

        // Si borras uno, vuelve a caber
        mockMvc.perform(delete(ubicacion)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/movimientos").contentType(MediaType.APPLICATION_JSON).content(MOVIMIENTO))
                .andExpect(status().isCreated());
    }

    @Test
    void alLlegarAlMaximoDeCategoriasNoDejaCrearMas() throws Exception {
        mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\": \"Gimnasio\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\": \"Regalos\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Has llegado al máximo de 8 categorías. Borra alguno para poder guardar más."));
    }

    @Test
    void alLlegarAlMaximoDeGastosFijosNoDejaCrearMas() throws Exception {
        String fijo = """
                {"descripcion": "Netflix", "tipo": "GASTO", "importe": 12.99,
                 "frecuencia": "MENSUAL", "fechaInicio": "2026-01-15"}
                """;
        mockMvc.perform(post("/api/pagos-recurrentes").contentType(MediaType.APPLICATION_JSON).content(fijo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/pagos-recurrentes").contentType(MediaType.APPLICATION_JSON).content(fijo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Has llegado al máximo de 1 gastos fijos. Borra alguno para poder guardar más."));
    }

    @Test
    void alLlegarAlMaximoDeSaldosNoDejaApuntarMas() throws Exception {
        String saldo = "{\"fecha\": \"2026-01-01\", \"importe\": 400}";
        mockMvc.perform(post("/api/saldo-banco").contentType(MediaType.APPLICATION_JSON).content(saldo))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(post("/api/saldo-banco").contentType(MediaType.APPLICATION_JSON).content(saldo))
                .andExpect(status().isConflict());
    }

    @Test
    void elLimiteEsDeCadaUsuarioYNoDeTodaLaApp() throws Exception {
        // Otro usuario (Ana) ya ha llenado SU cupo de gastos fijos (el máximo en este test es 1)...
        Usuario ana = usuarioService.entrarConGoogle(new DatosGoogle("google-ana", "ana@example.com", "Ana", null));
        pagoRecurrenteRepository.save(new PagoRecurrente(ana.getId(), "Gimnasio", TipoMovimiento.GASTO,
                new BigDecimal("30"), Frecuencia.CADA_4_SEMANAS, LocalDate.of(2026, 1, 15), true, null));

        // ...pero eso no le quita sitio al usuario de prueba: él todavía puede crear el suyo
        mockMvc.perform(post("/api/pagos-recurrentes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"descripcion": "Spotify", "tipo": "GASTO", "importe": 10.99,
                         "frecuencia": "MENSUAL", "fechaInicio": "2026-01-15"}
                        """))
                .andExpect(status().isCreated());
    }
}
