package com.markmadlang.finanzas.resumen;

import com.markmadlang.finanzas.categoria.Categoria;
import com.markmadlang.finanzas.categoria.CategoriaRepository;
import com.markmadlang.finanzas.movimiento.Movimiento;
import com.markmadlang.finanzas.movimiento.MovimientoRepository;
import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests del resumen mensual. @Transactional deshace los cambios al acabar cada test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResumenControllerTest {

    private static final String URL = "/api/resumen";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @BeforeEach
    void empezarSinMovimientos() {
        movimientoRepository.deleteAll();
    }

    private Categoria categoria(String nombre) {
        return categoriaRepository.findByNombreIgnoreCase(nombre).orElseThrow();
    }

    private void apuntar(TipoMovimiento tipo, String importe, String fecha, String descripcion, Categoria categoria) {
        movimientoRepository.save(new Movimiento(tipo, new BigDecimal(importe), LocalDate.parse(fecha),
                descripcion, categoria));
    }

    @Test
    void resumenSumaIngresosYGastosDelMesPorCategoria() throws Exception {
        apuntar(TipoMovimiento.INGRESO, "1500.00", "2026-11-01", "Nómina", categoria("Nómina"));
        apuntar(TipoMovimiento.INGRESO, "35.50", "2026-11-08", "Bote", null);
        apuntar(TipoMovimiento.GASTO, "650.00", "2026-11-01", "Alquiler", categoria("Vivienda"));
        apuntar(TipoMovimiento.GASTO, "45.90", "2026-11-03", "Súper", categoria("Comida"));
        apuntar(TipoMovimiento.GASTO, "12.36", "2026-11-30", "Panadería", categoria("Comida"));
        apuntar(TipoMovimiento.GASTO, "8.00", "2026-11-15", "Algo", null);
        // Estos dos son de otros meses: NO deben contar
        apuntar(TipoMovimiento.GASTO, "999.00", "2026-10-31", "Octubre", null);
        apuntar(TipoMovimiento.GASTO, "999.00", "2026-12-01", "Diciembre", null);

        mockMvc.perform(get(URL).param("mes", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-11"))
                .andExpect(jsonPath("$.totalIngresos").value(1535.50))
                .andExpect(jsonPath("$.totalGastos").value(716.26))
                .andExpect(jsonPath("$.saldo").value(819.24))
                // Ingresos, de mayor a menor
                .andExpect(jsonPath("$.ingresosPorCategoria", hasSize(2)))
                .andExpect(jsonPath("$.ingresosPorCategoria[0].categoria").value("Nómina"))
                .andExpect(jsonPath("$.ingresosPorCategoria[1].categoria").value("Sin categoría"))
                .andExpect(jsonPath("$.ingresosPorCategoria[1].categoriaId").doesNotExist())
                // Gastos, de mayor a menor (Comida = 45.90 + 12.36)
                .andExpect(jsonPath("$.gastosPorCategoria", hasSize(3)))
                .andExpect(jsonPath("$.gastosPorCategoria[0].categoria").value("Vivienda"))
                .andExpect(jsonPath("$.gastosPorCategoria[0].total").value(650.00))
                .andExpect(jsonPath("$.gastosPorCategoria[1].categoria").value("Comida"))
                .andExpect(jsonPath("$.gastosPorCategoria[1].total").value(58.26))
                .andExpect(jsonPath("$.gastosPorCategoria[2].categoria").value("Sin categoría"));
    }

    @Test
    void siGastasMasDeLoQueIngresasElSaldoEsNegativo() throws Exception {
        apuntar(TipoMovimiento.INGRESO, "500.00", "2026-11-01", "Bote", null);
        apuntar(TipoMovimiento.GASTO, "650.00", "2026-11-01", "Alquiler", categoria("Vivienda"));

        mockMvc.perform(get(URL).param("mes", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(-150.00));
    }

    @Test
    void mesSinMovimientosDevuelveCeros() throws Exception {
        mockMvc.perform(get(URL).param("mes", "2026-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIngresos").value(0))
                .andExpect(jsonPath("$.totalGastos").value(0))
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.ingresosPorCategoria", hasSize(0)))
                .andExpect(jsonPath("$.gastosPorCategoria", hasSize(0)));
    }

    @Test
    void sinDecirElMesUsaElActual() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value(YearMonth.now().toString()));
    }

    @Test
    void mesMalEscritoDevuelve400() throws Exception {
        mockMvc.perform(get(URL).param("mes", "noviembre"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.mes").value("El valor 'noviembre' no es válido"));

        mockMvc.perform(get(URL).param("mes", "2026-13"))
                .andExpect(status().isBadRequest());
    }
}
