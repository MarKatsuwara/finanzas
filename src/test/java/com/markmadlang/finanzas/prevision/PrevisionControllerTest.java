package com.markmadlang.finanzas.prevision;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la previsión del mes.
 * Usan un reloj PARADO en el 10/11/2026, así siempre dan el mismo resultado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({RelojParado.class, SesionDePrueba.class})
class PrevisionControllerTest {

    private static final String URL = "/api/prevision";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private PagoRecurrenteRepository pagoRecurrenteRepository;

    @Autowired
    private PrevisionService previsionService;

    @BeforeEach
    void empezarDeCero() {
        movimientoRepository.deleteAll();
        pagoRecurrenteRepository.deleteAll();
    }

    private void apuntar(TipoMovimiento tipo, String importe, String fecha, String descripcion) {
        movimientoRepository.save(new Movimiento(tipo, new BigDecimal(importe), LocalDate.parse(fecha),
                descripcion, null));
    }

    private void recurrente(String descripcion, TipoMovimiento tipo, String importe, Frecuencia frecuencia,
                            String inicio, boolean activo) {
        pagoRecurrenteRepository.save(new PagoRecurrente(descripcion, tipo, new BigDecimal(importe), frecuencia,
                LocalDate.parse(inicio), activo, null));
    }

    @Test
    void previsionJuntaLoApuntadoConLoQueFaltaHastaFinDeMes() throws Exception {
        apuntar(TipoMovimiento.INGRESO, "1500.00", "2026-11-01", "Nómina");
        apuntar(TipoMovimiento.GASTO, "650.00", "2026-11-01", "Alquiler");
        recurrente("Netflix", TipoMovimiento.GASTO, "12.99", Frecuencia.MENSUAL, "2026-03-15", true); // 15/11
        recurrente("Alquiler", TipoMovimiento.GASTO, "650.00", Frecuencia.MENSUAL, "2026-01-01", true); // 01/12: no entra
        recurrente("Bote", TipoMovimiento.INGRESO, "30.00", Frecuencia.SEMANAL, "2026-11-03", true); // 17/11 y 24/11
        recurrente("Gimnasio", TipoMovimiento.GASTO, "30.00", Frecuencia.MENSUAL, "2026-01-20", false); // de baja

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-11"))
                .andExpect(jsonPath("$.hoy").value("2026-11-10"))
                .andExpect(jsonPath("$.ingresosApuntados").value(1500.00))
                .andExpect(jsonPath("$.gastosApuntados").value(650.00))
                .andExpect(jsonPath("$.ingresosPendientes").value(60.00))
                .andExpect(jsonPath("$.gastosPendientes").value(12.99))
                // (1500 + 60) - (650 + 12.99) = 897.01
                .andExpect(jsonPath("$.saldoPrevisto").value(897.01))
                .andExpect(jsonPath("$.numerosRojos").value(false))
                .andExpect(jsonPath("$.mensaje").value("Vas bien: acabarás el mes con 897.01 € de margen"))
                .andExpect(jsonPath("$.pagosPendientes", hasSize(3)))
                .andExpect(jsonPath("$.pagosPendientes[0].descripcion").value("Netflix"))
                .andExpect(jsonPath("$.pagosPendientes[1].fecha").value("2026-11-17"))
                .andExpect(jsonPath("$.pagosPendientes[2].fecha").value("2026-11-24"));
    }

    @Test
    void siVasAGastarMasDeLoQueIngresasAvisaDeNumerosRojos() throws Exception {
        apuntar(TipoMovimiento.INGRESO, "500.00", "2026-11-05", "Bote");
        apuntar(TipoMovimiento.GASTO, "400.00", "2026-11-06", "Compras");
        recurrente("Seguro", TipoMovimiento.GASTO, "200.00", Frecuencia.ANUAL, "2025-11-20", true); // 20/11

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoPrevisto").value(-100.00))
                .andExpect(jsonPath("$.numerosRojos").value(true))
                .andExpect(jsonPath("$.mensaje").value(containsString("gastarás 100.00 € más de lo que ingresas")));
    }

    @Test
    void losPagosDeHoyNoCuentanComoPendientes() throws Exception {
        // Toca justo hoy (10/11): se supone que ya está apuntado como movimiento
        recurrente("Spotify", TipoMovimiento.GASTO, "10.99", Frecuencia.MENSUAL, "2026-01-10", true);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gastosPendientes").value(0))
                .andExpect(jsonPath("$.pagosPendientes", hasSize(0)));
    }

    @Test
    void sinDatosTodoEsCeroYNoHayNumerosRojos() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoPrevisto").value(0))
                .andExpect(jsonPath("$.numerosRojos").value(false));
    }

    @Test
    void elUltimoDiaDelMesYaNoQuedanPagosPendientes() {
        recurrente("Netflix", TipoMovimiento.GASTO, "12.99", Frecuencia.SEMANAL, "2026-11-01", true);

        PrevisionResponse prevision = previsionService.prevision(LocalDate.of(2026, 11, 30));

        assertThat(prevision.pagosPendientes()).isEmpty();
        assertThat(prevision.gastosPendientes()).isEqualByComparingTo("0");
    }
}
