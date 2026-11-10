package com.markmadlang.finanzas.movimiento;

import com.markmadlang.finanzas.categoria.CategoriaResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que DEVOLVEMOS al usuario. Separarlo de la entidad nos deja cambiar
 * la base de datos sin romper lo que ve la web.
 */
public record MovimientoResponse(
        Long id,
        TipoMovimiento tipo,
        BigDecimal importe,
        LocalDate fecha,
        String descripcion,
        CategoriaResponse categoria // null si no tiene
) {

    static MovimientoResponse desde(Movimiento m) {
        return new MovimientoResponse(m.getId(), m.getTipo(), m.getImporte(), m.getFecha(), m.getDescripcion(),
                CategoriaResponse.desde(m.getCategoria()));
    }
}
