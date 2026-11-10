package com.markmadlang.finanzas.prevision;

import com.markmadlang.finanzas.pagorecurrente.ProximoPagoResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Cómo vas a acabar el mes:
 *   (ingresos apuntados + ingresos pendientes) - (gastos apuntados + gastos pendientes)
 */
public record PrevisionResponse(
        YearMonth mes,
        LocalDate hoy,
        BigDecimal ingresosApuntados,          // movimientos de este mes
        BigDecimal gastosApuntados,
        BigDecimal ingresosPendientes,         // pagos recurrentes de mañana a fin de mes
        BigDecimal gastosPendientes,
        BigDecimal saldoPrevisto,              // con lo que acabarás el mes
        boolean numerosRojos,                  // true = vas a gastar más de lo que ingresas
        String mensaje,
        List<ProximoPagoResponse> pagosPendientes
) {
}
