package com.markmadlang.finanzas.movimiento;

import java.math.BigDecimal;

/**
 * Una fila del resultado de la consulta de totales:
 * "los GASTOS de la categoría Comida suman 158,26 €".
 * categoriaId y categoriaNombre son null para los movimientos sin categoría.
 */
public record TotalPorCategoria(
        TipoMovimiento tipo,
        Long categoriaId,
        String categoriaNombre,
        BigDecimal total
) {
}
