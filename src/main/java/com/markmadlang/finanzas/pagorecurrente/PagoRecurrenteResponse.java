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
        CategoriaResponse categoria,
        BigDecimal importeMensual,  // lo que cuesta repartido por meses (420 €/año → 35,00)
        LocalDate proximaFecha,     // la próxima vez que toca (null si está desactivado)
        boolean apuntarSolo         // true = cuando llega su día, la app lo apunta sola como movimiento
) {

    static PagoRecurrenteResponse desde(PagoRecurrente p, LocalDate hoy) {
        return new PagoRecurrenteResponse(p.getId(), p.getDescripcion(), p.getTipo(), p.getImporte(),
                p.getFrecuencia(), p.getFechaInicio(), p.isActivo(), CategoriaResponse.desde(p.getCategoria()),
                p.importeMensual(), p.isActivo() ? p.proximaFechaDesde(hoy) : null, p.isApuntarSolo());
    }
}
