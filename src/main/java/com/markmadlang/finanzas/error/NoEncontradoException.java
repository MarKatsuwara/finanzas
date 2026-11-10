package com.markmadlang.finanzas.error;

/**
 * Error general para "eso que buscas no existe". Acaba convertido en un 404.
 * Las excepciones concretas (movimiento, categoría...) heredan de esta.
 */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
