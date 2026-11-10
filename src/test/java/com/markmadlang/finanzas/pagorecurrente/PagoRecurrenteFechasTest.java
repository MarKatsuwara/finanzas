package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests "unitarios" del cálculo de fechas: no arrancan Spring ni base de datos,
 * solo prueban la lógica en Java puro. Por eso son rapidísimos.
 */
class PagoRecurrenteFechasTest {

    private static PagoRecurrente pago(Frecuencia frecuencia, LocalDate inicio) {
        return new PagoRecurrente("Prueba", TipoMovimiento.GASTO, BigDecimal.TEN, frecuencia, inicio, true, null);
    }

    private static LocalDate fecha(int anio, int mes, int dia) {
        return LocalDate.of(anio, mes, dia);
    }

    @Test
    void mensualDevuelveUnaFechaPorMes() {
        PagoRecurrente alquiler = pago(Frecuencia.MENSUAL, fecha(2026, 1, 1));

        assertThat(alquiler.fechasEntre(fecha(2026, 11, 10), fecha(2027, 1, 31)))
                .containsExactly(fecha(2026, 12, 1), fecha(2027, 1, 1));
    }

    @Test
    void mensualDelDia31UsaElUltimoDiaDelMesYVuelveAl31() {
        PagoRecurrente pago = pago(Frecuencia.MENSUAL, fecha(2026, 1, 31));

        assertThat(pago.fechasEntre(fecha(2026, 1, 1), fecha(2026, 4, 30)))
                .containsExactly(
                        fecha(2026, 1, 31),
                        fecha(2026, 2, 28), // febrero no tiene 31 → último día
                        fecha(2026, 3, 31), // y en marzo vuelve al 31
                        fecha(2026, 4, 30));
    }

    @Test
    void anualDel29DeFebreroUsaEl28EnAniosNoBisiestos() {
        PagoRecurrente pago = pago(Frecuencia.ANUAL, fecha(2028, 2, 29)); // 2028 es bisiesto

        assertThat(pago.fechasEntre(fecha(2028, 1, 1), fecha(2032, 12, 31)))
                .containsExactly(
                        fecha(2028, 2, 29),
                        fecha(2029, 2, 28),
                        fecha(2030, 2, 28),
                        fecha(2031, 2, 28),
                        fecha(2032, 2, 29)); // 2032 vuelve a ser bisiesto
    }

    @Test
    void semanalPuedeTocarVariasVecesEnElPeriodo() {
        PagoRecurrente pago = pago(Frecuencia.SEMANAL, fecha(2026, 11, 2)); // un lunes

        assertThat(pago.fechasEntre(fecha(2026, 11, 10), fecha(2026, 11, 30)))
                .containsExactly(fecha(2026, 11, 16), fecha(2026, 11, 23), fecha(2026, 11, 30));
    }

    @Test
    void lasDosFechasDelPeriodoCuentan() {
        PagoRecurrente pago = pago(Frecuencia.MENSUAL, fecha(2026, 1, 10));

        assertThat(pago.fechasEntre(fecha(2026, 11, 10), fecha(2026, 12, 10)))
                .containsExactly(fecha(2026, 11, 10), fecha(2026, 12, 10));
    }

    @Test
    void siEmpiezaDespuesDelPeriodoNoDevuelveNada() {
        PagoRecurrente pago = pago(Frecuencia.MENSUAL, fecha(2027, 6, 1));

        assertThat(pago.fechasEntre(fecha(2026, 11, 10), fecha(2026, 12, 10))).isEmpty();
    }

    @Test
    void siEmpiezaDentroDelPeriodoNoInventaFechasAnteriores() {
        PagoRecurrente pago = pago(Frecuencia.SEMANAL, fecha(2026, 11, 20));

        assertThat(pago.fechasEntre(fecha(2026, 11, 1), fecha(2026, 11, 30)))
                .containsExactly(fecha(2026, 11, 20), fecha(2026, 11, 27));
    }
}
