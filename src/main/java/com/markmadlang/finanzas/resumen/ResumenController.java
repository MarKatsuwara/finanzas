package com.markmadlang.finanzas.resumen;

import com.markmadlang.finanzas.fecha.Fechas;
import com.markmadlang.finanzas.seguridad.UsuarioId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.YearMonth;

/** La "puerta" de la API para los resúmenes. */
@RestController
@RequestMapping("/api/resumen")
public class ResumenController {

    private final ResumenService service;
    private final Clock reloj;

    public ResumenController(ResumenService service, Clock reloj) {
        this.service = service;
        this.reloj = reloj;
    }

    /**
     * Resumen de un mes. Ejemplos:
     *   /api/resumen              → el mes actual
     *   /api/resumen?mes=2026-11  → noviembre de 2026
     */
    @GetMapping
    public ResumenMensualResponse resumenDelMes(
            @UsuarioId Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        Fechas.comprobar("mes", mes);
        // El mes actual según el reloj de la app (hora de España), no el del servidor
        return service.resumenDelMes(usuarioId, mes != null ? mes : YearMonth.now(reloj));
    }
}
