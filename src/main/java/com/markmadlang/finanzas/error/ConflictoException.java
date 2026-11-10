package com.markmadlang.finanzas.error;

/**
 * La petición choca con lo que ya hay guardado
 * (nombre repetido, borrar algo que se está usando...). Acaba convertido en un 409.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
