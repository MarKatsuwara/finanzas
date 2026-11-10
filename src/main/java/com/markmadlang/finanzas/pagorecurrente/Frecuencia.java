package com.markmadlang.finanzas.pagorecurrente;

import java.time.LocalDate;

/**
 * Cada cuánto se repite un pago.
 * Cada frecuencia sabe dos cosas: cuántas veces se paga al año
 * y cómo calcular la fecha de la repetición número N.
 */
public enum Frecuencia {
    SEMANAL(52),
    CADA_4_SEMANAS(13), // cada 28 días: 13 veces al año, y algún mes toca dos veces
    MENSUAL(12),
    BIMESTRAL(6),   // cada 2 meses
    TRIMESTRAL(4),  // cada 3 meses
    SEMESTRAL(2),   // cada 6 meses
    ANUAL(1);

    private final int vecesAlAnio;

    Frecuencia(int vecesAlAnio) {
        this.vecesAlAnio = vecesAlAnio;
    }

    /** Cuántas veces se paga en un año (semanal = 52, mensual = 12, anual = 1...). */
    public int vecesAlAnio() {
        return vecesAlAnio;
    }

    /**
     * Devuelve la fecha de la repetición número "veces", contando desde la fecha de inicio.
     * Ejemplo con MENSUAL e inicio 31/01: veces=0 → 31/01, veces=1 → 28/02, veces=2 → 31/03.
     *
     * Siempre se cuenta DESDE EL INICIO (y no desde la fecha anterior) para que un pago
     * del día 31 vuelva al 31 en marzo, en lugar de quedarse en el 28 para siempre.
     * Si el mes no tiene ese día, Java usa el último día del mes.
     */
    public LocalDate repeticion(LocalDate inicio, long veces) {
        return switch (this) {
            case SEMANAL -> inicio.plusWeeks(veces);
            case CADA_4_SEMANAS -> inicio.plusWeeks(4 * veces);
            case MENSUAL -> inicio.plusMonths(veces);
            case BIMESTRAL -> inicio.plusMonths(2 * veces);
            case TRIMESTRAL -> inicio.plusMonths(3 * veces);
            case SEMESTRAL -> inicio.plusMonths(6 * veces);
            case ANUAL -> inicio.plusYears(veces);
        };
    }
}
