package com.markmadlang.finanzas.fecha;

import com.markmadlang.finanzas.error.CampoNoValidoException;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Las fechas que admite la app: del 01/01/2000 al 31/12/2100.
 *
 * ¿Por qué? Sin límite, la API aceptaba cosas como el año 1 o el año 10000. Eso no es un dato real
 * (es un error al teclear o alguien probando), y además un gasto semanal desde el año 1 obliga a
 * calcular 100.000 semanas en cada petición, o hace que Java se pase de la fecha máxima y falle (error 500).
 */
public final class Fechas {

    public static final LocalDate MINIMA = LocalDate.of(2000, 1, 1);
    public static final LocalDate MAXIMA = LocalDate.of(2100, 12, 31);
    public static final String MENSAJE = "La fecha debe estar entre el 01/01/2000 y el 31/12/2100";

    private Fechas() {
    }

    /** ¿Está dentro del rango? (null cuenta como válida: de eso se encarga @NotNull) */
    public static boolean esRazonable(LocalDate fecha) {
        return fecha == null || (!fecha.isBefore(MINIMA) && !fecha.isAfter(MAXIMA));
    }

    /** Para los datos que llegan por la URL (?desde=...): si no es razonable, 400 en ese campo. */
    public static void comprobar(String campo, LocalDate fecha) {
        if (!esRazonable(fecha)) {
            throw new CampoNoValidoException(campo, MENSAJE);
        }
    }

    /** Lo mismo para un mes (?mes=2026-11). */
    public static void comprobar(String campo, YearMonth mes) {
        if (mes != null && !(esRazonable(mes.atDay(1)) && esRazonable(mes.atEndOfMonth()))) {
            throw new CampoNoValidoException(campo, MENSAJE);
        }
    }
}
