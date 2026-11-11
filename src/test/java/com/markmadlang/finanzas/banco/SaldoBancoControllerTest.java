package com.markmadlang.finanzas.banco;

import com.markmadlang.finanzas.RelojParado;
import com.markmadlang.finanzas.SesionDePrueba;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests del saldo del banco.
 * Usan un reloj PARADO: para la app, "hoy" siempre es el 10/11/2026.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({RelojParado.class, SesionDePrueba.class})
class SaldoBancoControllerTest {

    private static final String URL = "/api/saldo-banco";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SaldoBancoRepository repository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private PagoRecurrenteRepository pagoRecurrenteRepository;

    @BeforeEach
    void empezarDeCero() {
        repository.deleteAll();
        movimientoRepository.deleteAll();
        pagoRecurrenteRepository.deleteAll();
    }

    private void saldo(String fecha, String importe) {
        repository.save(new SaldoBanco(LocalDate.parse(fecha), new BigDecimal(importe)));
    }

    private void apuntar(TipoMovimiento tipo, String importe, String fecha, String descripcion) {
        movimientoRepository.save(new Movimiento(tipo, new BigDecimal(importe), LocalDate.parse(fecha),
                descripcion, null));
    }

    private void recurrente(String descripcion, TipoMovimiento tipo, String importe, String inicio) {
        pagoRecurrenteRepository.save(new PagoRecurrente(descripcion, tipo, new BigDecimal(importe),
                Frecuencia.MENSUAL, LocalDate.parse(inicio), true, null));
    }

    // ---------- Apuntar el saldo ----------

    @Test
    void apuntarSaldoDevuelve201() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2026-11-10\", \"importe\": 400}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fecha").value("2026-11-10"))
                .andExpect(jsonPath("$.importe").value(400.00));
    }

    @Test
    void apuntarSaldoNegativoEstaPermitido() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2026-11-10\", \"importe\": -35.20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importe").value(-35.20));
    }

    @Test
    void apuntarSinDatosDevuelve400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha").exists())
                .andExpect(jsonPath("$.errores.importe").exists());
    }

    @Test
    void apuntarConFechaFuturaDevuelve400() throws Exception {
        String futura = LocalDate.now().plusDays(5).toString();

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"" + futura + "\", \"importe\": 400}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha").exists());
    }

    @Test
    void historialDevuelveDelMasRecienteAlMasAntiguo() throws Exception {
        saldo("2026-11-01", "100.00");
        saldo("2026-11-08", "300.00");

        mockMvc.perform(get(URL + "/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fecha").value("2026-11-08"))
                .andExpect(jsonPath("$[1].fecha").value("2026-11-01"));
    }

    // ---------- Saldo de hoy ----------

    @Test
    void sinSaldoApuntadoDevuelve404() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Todavía no has apuntado")));

        mockMvc.perform(get(URL + "/prevision"))
                .andExpect(status().isNotFound());
    }

    @Test
    void saldoDeHoySumaYRestaLosMovimientosPosterioresAlApuntado() throws Exception {
        saldo("2026-11-05", "400.00");
        apuntar(TipoMovimiento.GASTO, "20.00", "2026-11-05", "Mismo día: ya está en los 400");
        apuntar(TipoMovimiento.INGRESO, "100.00", "2026-11-07", "Bote");
        apuntar(TipoMovimiento.GASTO, "30.00", "2026-11-08", "Cena");
        apuntar(TipoMovimiento.GASTO, "50.00", "2026-11-12", "Futuro: todavía no ha pasado");

        // 400 + 100 - 30 = 470
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoy").value("2026-11-10"))
                .andExpect(jsonPath("$.saldoApuntado.importe").value(400.00))
                .andExpect(jsonPath("$.ingresosDesde").value(100.00))
                .andExpect(jsonPath("$.gastosDesde").value(30.00))
                .andExpect(jsonPath("$.saldoHoy").value(470.00));
    }

    @Test
    void seUsaElSaldoApuntadoMasReciente() throws Exception {
        saldo("2026-11-01", "100.00");
        saldo("2026-11-08", "300.00");
        apuntar(TipoMovimiento.GASTO, "999.00", "2026-11-05", "Antes del último saldo: no cuenta");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoApuntado.fecha").value("2026-11-08"))
                .andExpect(jsonPath("$.saldoHoy").value(300.00));
    }

    @Test
    void siElSaldoEsDeHoyNoSeSumaNada() throws Exception {
        saldo("2026-11-10", "250.00");
        apuntar(TipoMovimiento.GASTO, "10.00", "2026-11-10", "Hoy: ya está en los 250");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoHoy").value(250.00));
    }

    // ---------- Previsión día a día ----------

    @Test
    void previsionAvisaDelDiaEnQueTeQuedasEnNegativo() throws Exception {
        saldo("2026-11-10", "400.00");
        recurrente("Netflix", TipoMovimiento.GASTO, "12.99", "2026-03-15");   // 15/11
        recurrente("Alquiler", TipoMovimiento.GASTO, "650.00", "2026-01-01"); // 01/12
        apuntar(TipoMovimiento.GASTO, "30.00", "2026-11-20", "Regalo");       // movimiento futuro

        // 400 → 15/11: 387.01 → 20/11: 357.01 → 01/12: -292.99
        mockMvc.perform(get(URL + "/prevision").param("dias", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoHoy").value(400.00))
                .andExpect(jsonPath("$.hasta").value("2026-12-10"))
                .andExpect(jsonPath("$.dias", hasSize(3)))
                .andExpect(jsonPath("$.dias[0].fecha").value("2026-11-15"))
                .andExpect(jsonPath("$.dias[0].saldoAlFinal").value(387.01))
                .andExpect(jsonPath("$.dias[0].movimientos[0].origen").value("PAGO_RECURRENTE"))
                .andExpect(jsonPath("$.dias[1].fecha").value("2026-11-20"))
                .andExpect(jsonPath("$.dias[1].movimientos[0].origen").value("MOVIMIENTO"))
                .andExpect(jsonPath("$.dias[2].fecha").value("2026-12-01"))
                .andExpect(jsonPath("$.dias[2].saldoAlFinal").value(-292.99))
                .andExpect(jsonPath("$.saldoFinal").value(-292.99))
                .andExpect(jsonPath("$.saldoMinimo").value(-292.99))
                .andExpect(jsonPath("$.primerDiaEnNegativo").value("2026-12-01"))
                .andExpect(jsonPath("$.numerosRojos").value(true))
                .andExpect(jsonPath("$.mensaje").value("Cuidado: el 01/12/2026 te quedarás en -292.99 €"));
    }

    @Test
    void siLaNominaLlegaElMismoDiaQueElAlquilerNoHayNumerosRojos() throws Exception {
        saldo("2026-11-10", "400.00");
        recurrente("Alquiler", TipoMovimiento.GASTO, "650.00", "2026-01-01");  // 01/12
        recurrente("Nómina", TipoMovimiento.INGRESO, "1500.00", "2026-01-01"); // 01/12 también

        // Se mira el saldo al FINAL del día: 400 - 650 + 1500 = 1250
        mockMvc.perform(get(URL + "/prevision").param("dias", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias", hasSize(1)))
                .andExpect(jsonPath("$.dias[0].movimientos", hasSize(2)))
                .andExpect(jsonPath("$.dias[0].saldoAlFinal").value(1250.00))
                .andExpect(jsonPath("$.saldoMinimo").value(400.00))
                .andExpect(jsonPath("$.primerDiaEnNegativo").doesNotExist())
                .andExpect(jsonPath("$.numerosRojos").value(false))
                .andExpect(jsonPath("$.mensaje").value("Vas bien: en los próximos 30 días tu saldo no bajará de 400.00 €"));
    }

    @Test
    void siYaEstasEnNegativoAvisaDesdeHoy() throws Exception {
        saldo("2026-11-10", "-20.00");

        mockMvc.perform(get(URL + "/prevision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primerDiaEnNegativo").value("2026-11-10"))
                .andExpect(jsonPath("$.mensaje").value("Cuidado: ya estás en negativo (-20.00 €)"));
    }

    @Test
    void previsionConDiasFueraDeRangoDevuelve400() throws Exception {
        saldo("2026-11-10", "400.00");

        mockMvc.perform(get(URL + "/prevision").param("dias", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.dias").exists());
    }
}
