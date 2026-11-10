package com.markmadlang.finanzas.banco;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cuánto tienes hoy:
 *   saldo apuntado + ingresos desde entonces - gastos desde entonces
 */
public record SaldoActualResponse(
        LocalDate hoy,
        SaldoApuntadoResponse saldoApuntado,  // el punto de partida
        BigDecimal ingresosDesde,             // movimientos del día siguiente al apuntado hasta hoy
        BigDecimal gastosDesde,
        BigDecimal saldoHoy
) {
}
