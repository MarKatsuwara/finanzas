package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.categoria.CategoriaResponse;
import com.markmadlang.finanzas.movimiento.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un pago que toca en una fecha concreta: "el 01/12 pagas el Alquiler, 650 €". */
public record ProximoPagoResponse(
        LocalDate fecha,
        Long pagoRecurrenteId,
        String descripcion,
        TipoMovimiento tipo,
        BigDecimal importe,
        CategoriaResponse categoria
) {

    static ProximoPagoResponse desde(PagoRecurrente p, LocalDate fecha) {
        return new ProximoPagoResponse(fecha, p.getId(), p.getDescripcion(), p.getTipo(), p.getImporte(),
                CategoriaResponse.desde(p.getCategoria()));
    }
}
