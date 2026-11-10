package com.markmadlang.finanzas.movimiento;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que nos ENVÍA el usuario para crear o editar un movimiento.
 * Las anotaciones son las reglas: si alguna no se cumple, la API responde 400 (Bad Request).
 */
public record MovimientoRequest(

        @NotNull(message = "El tipo es obligatorio (INGRESO o GASTO)")
        TipoMovimiento tipo,

        @NotNull(message = "El importe es obligatorio")
        @Positive(message = "El importe debe ser mayor que 0")
        @Digits(integer = 10, fraction = 2, message = "El importe admite como máximo 10 cifras enteras y 2 decimales")
        BigDecimal importe,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255, message = "La descripción no puede pasar de 255 caracteres")
        String descripcion,

        // Opcional: si no se envía, el movimiento queda sin categoría
        Long categoriaId
) {
}
