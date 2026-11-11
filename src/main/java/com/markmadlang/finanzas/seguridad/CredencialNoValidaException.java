package com.markmadlang.finanzas.seguridad;

/** El carné de Google no es válido (falso, caducado o de otra app). Acaba convertido en un 401. */
public class CredencialNoValidaException extends RuntimeException {

    public CredencialNoValidaException() {
        super("No hemos podido comprobar tu cuenta de Google. Vuelve a intentarlo.");
    }
}
