package com.markmadlang.finanzas.movimiento;

import com.markmadlang.finanzas.error.NoEncontradoException;

/** Se lanza cuando alguien pide un movimiento que no existe. Acaba convertido en un 404. */
public class MovimientoNoEncontradoException extends NoEncontradoException {

    public MovimientoNoEncontradoException(Long id) {
        super("No existe ningún movimiento con id " + id);
    }
}
