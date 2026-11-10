package com.markmadlang.finanzas.resumen;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** El resumen de un mes: cuánto ha entrado, cuánto ha salido y en qué. */
public record ResumenMensualResponse(
        YearMonth mes,                                   // se ve como "2026-11"
        BigDecimal totalIngresos,
        BigDecimal totalGastos,
        BigDecimal saldo,                                // ingresos - gastos (negativo = has gastado más)
        List<TotalCategoriaResponse> ingresosPorCategoria,
        List<TotalCategoriaResponse> gastosPorCategoria
) {
}
