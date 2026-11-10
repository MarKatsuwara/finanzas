package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lo que nos envían para crear o editar un pago recurrente. */
public record PagoRecurrenteRequest(

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255, message = "La descripción no puede pasar de 255 caracteres")
        String descripcion,

        @NotNull(message = "El tipo es obligatorio (INGRESO o GASTO)")
        TipoMovimiento tipo,

        @NotNull(message = "El importe es obligatorio")
        @Positive(message = "El importe debe ser mayor que 0")
        @Digits(integer = 10, fraction = 2, message = "El importe admite como máximo 10 cifras enteras y 2 decimales")
        BigDecimal importe,

        @NotNull(message = "La frecuencia es obligatoria (SEMANAL, CADA_4_SEMANAS, MENSUAL, BIMESTRAL, TRIMESTRAL, SEMESTRAL o ANUAL)")
        Frecuencia frecuencia,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        // Opcional. Al crear, si no se envía, queda activo.
        // Al editar, si no se envía, se queda como estaba.
        Boolean activo,

        // Opcional, igual que en los movimientos
        Long categoriaId
) {
}
