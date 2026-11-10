package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.error.NoEncontradoException;

/** Se lanza cuando alguien pide un pago recurrente que no existe. Acaba convertido en un 404. */
public class PagoRecurrenteNoEncontradoException extends NoEncontradoException {

    public PagoRecurrenteNoEncontradoException(Long id) {
        super("No existe ningún pago recurrente con id " + id);
    }
}
