package com.markmadlang.finanzas.movimiento;

/** Se lanza cuando alguien pide un id que no existe. Acaba convertido en un 404. */
public class MovimientoNoEncontradoException extends RuntimeException {

    public MovimientoNoEncontradoException(Long id) {
        super("No existe ningún movimiento con id " + id);
    }
}
