package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.categoria.CategoriaResponse;
import com.markmadlang.finanzas.movimiento.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lo que devolvemos de un pago recurrente. */
public record PagoRecurrenteResponse(
        Long id,
        String descripcion,
        TipoMovimiento tipo,
        BigDecimal importe,
        Frecuencia frecuencia,
        LocalDate fechaInicio,
        boolean activo,
        CategoriaResponse categoria
) {

    static PagoRecurrenteResponse desde(PagoRecurrente p) {
        return new PagoRecurrenteResponse(p.getId(), p.getDescripcion(), p.getTipo(), p.getImporte(),
                p.getFrecuencia(), p.getFechaInicio(), p.isActivo(), CategoriaResponse.desde(p.getCategoria()));
    }
}
