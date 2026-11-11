package com.markmadlang.finanzas.seguridad;

/** Al servidor le falta el ID de cliente de Google, así que no puede comprobar los carnés. Acaba en un 500 con este mensaje. */
public class GoogleSinConfigurarException extends RuntimeException {

    public GoogleSinConfigurarException() {
        super("El servidor no tiene configurado el ID de cliente de Google (variable GOOGLE_CLIENT_ID)");
    }
}
