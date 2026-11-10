package com.markmadlang.finanzas.error;

/**
 * Un campo tiene buen formato pero un valor imposible
 * (por ejemplo, una categoría que no existe). Acaba convertido en un 400.
 */
public class CampoNoValidoException extends RuntimeException {

    private final String campo;

    public CampoNoValidoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
