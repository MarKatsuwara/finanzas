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
        return new PagoRecurrente(1L, "Prueba", TipoMovimiento.GASTO, BigDecimal.TEN, frecuencia, inicio, true, null);
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
    void cadaCuatroSemanasPuedeTocarDosVecesElMismoMes() {
        PagoRecurrente gimnasio = pago(Frecuencia.CADA_4_SEMANAS, fecha(2026, 1, 5));

        assertThat(gimnasio.fechasEntre(fecha(2026, 1, 1), fecha(2026, 4, 30)))
                .containsExactly(
                        fecha(2026, 1, 5),
                        fecha(2026, 2, 2),
                        fecha(2026, 3, 2),
                        fecha(2026, 3, 30), // ¡dos veces en marzo!
                        fecha(2026, 4, 27));
    }

    @Test
    void cadaCuatroSemanasSonTreceVecesAlAnio() {
        PagoRecurrente gimnasio = new PagoRecurrente(1L, "Gimnasio", TipoMovimiento.GASTO, new BigDecimal("30.00"),
                Frecuencia.CADA_4_SEMANAS, fecha(2026, 1, 5), true, null);

        assertThat(gimnasio.fechasEntre(fecha(2026, 1, 5), fecha(2027, 1, 3))).hasSize(13);
        assertThat(gimnasio.importeMensual()).isEqualByComparingTo("32.50"); // 30 × 13 / 12
    }

    @Test
    void bimestralTocaCadaDosMeses() {
        PagoRecurrente luz = pago(Frecuencia.BIMESTRAL, fecha(2026, 1, 10));

        assertThat(luz.fechasEntre(fecha(2026, 1, 1), fecha(2026, 8, 31)))
                .containsExactly(fecha(2026, 1, 10), fecha(2026, 3, 10), fecha(2026, 5, 10), fecha(2026, 7, 10));
    }

    @Test
    void trimestralDelDia31UsaElUltimoDiaDelMes() {
        PagoRecurrente pago = pago(Frecuencia.TRIMESTRAL, fecha(2026, 1, 31));

        assertThat(pago.fechasEntre(fecha(2026, 1, 1), fecha(2026, 12, 31)))
                .containsExactly(
                        fecha(2026, 1, 31),
                        fecha(2026, 4, 30),  // abril no tiene 31
                        fecha(2026, 7, 31),
                        fecha(2026, 10, 31));
    }

    @Test
    void semestralDel31DeAgostoCaeEnFebreroYVuelveAAgosto() {
        PagoRecurrente pago = pago(Frecuencia.SEMESTRAL, fecha(2026, 8, 31));

        assertThat(pago.fechasEntre(fecha(2026, 1, 1), fecha(2027, 12, 31)))
                .containsExactly(fecha(2026, 8, 31), fecha(2027, 2, 28), fecha(2027, 8, 31));
    }

    @Test
    void proximaFechaEsLaPrimeraDesdeHoy() {
        PagoRecurrente alquiler = pago(Frecuencia.MENSUAL, fecha(2026, 1, 1));

        assertThat(alquiler.proximaFechaDesde(fecha(2026, 11, 10))).isEqualTo(fecha(2026, 12, 1));
        assertThat(alquiler.proximaFechaDesde(fecha(2026, 12, 1))).isEqualTo(fecha(2026, 12, 1)); // si toca hoy, es hoy
    }

    @Test
    void siEmpiezaEnElFuturoLaProximaFechaEsElInicio() {
        PagoRecurrente pago = pago(Frecuencia.ANUAL, fecha(2027, 3, 15));

        assertThat(pago.proximaFechaDesde(fecha(2026, 11, 10))).isEqualTo(fecha(2027, 3, 15));
    }

    @Test
    void importeMensualRepartePorMesesYRedondea() {
        PagoRecurrente semestral = new PagoRecurrente(1L, "Matrícula", TipoMovimiento.GASTO, new BigDecimal("100.00"),
                Frecuencia.SEMESTRAL, fecha(2026, 1, 1), true, null);

        assertThat(semestral.importeMensual()).isEqualByComparingTo("16.67"); // 200 / 12 = 16,666...
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
