package com.markmadlang.finanzas.movimiento;

/**
 * Los dos únicos tipos de movimiento que existen.
 * Usar un enum evita errores como escribir "ingreso" o "GASTOS".
 */
public enum TipoMovimiento {
    INGRESO,
    GASTO
}
