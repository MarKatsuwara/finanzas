package com.markmadlang.finanzas.fecha;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

/**
 * Regla para los datos que nos envían: la fecha debe estar entre 2000 y 2100 (ver Fechas).
 * Se usa igual que @NotNull: se pone encima del campo y, si no se cumple, la API responde 400.
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FechaRazonable.Comprobador.class)
public @interface FechaRazonable {

    String message() default Fechas.MENSAJE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** El que hace la comprobación de verdad. */
    class Comprobador implements ConstraintValidator<FechaRazonable, LocalDate> {
        @Override
        public boolean isValid(LocalDate fecha, ConstraintValidatorContext contexto) {
            return Fechas.esRazonable(fecha);
        }
    }
}
