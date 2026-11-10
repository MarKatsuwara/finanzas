package com.markmadlang.finanzas.pagorecurrente;

import java.time.LocalDate;

/** Cada cuánto se repite un pago. */
public enum Frecuencia {
    SEMANAL,
    MENSUAL,
    ANUAL;

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
            case MENSUAL -> inicio.plusMonths(veces);
            case ANUAL -> inicio.plusYears(veces);
        };
    }
}
