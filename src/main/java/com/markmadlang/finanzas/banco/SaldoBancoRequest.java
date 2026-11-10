package com.markmadlang.finanzas.banco;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lo que nos envían para apuntar el saldo del banco. */
public record SaldoBancoRequest(

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura: apunta lo que tienes hoy o tenías otro día")
        LocalDate fecha,

        // Sin @Positive: si estás en descubierto, el saldo puede ser negativo
        @NotNull(message = "El importe es obligatorio")
        @Digits(integer = 10, fraction = 2, message = "El importe admite como máximo 10 cifras enteras y 2 decimales")
        BigDecimal importe
) {
}
