package com.markmadlang.finanzas.banco;

import com.markmadlang.finanzas.error.NoEncontradoException;

/** Se lanza si se pide el saldo pero nunca se ha apuntado ninguno. Acaba convertido en un 404. */
public class SaldoNoApuntadoException extends NoEncontradoException {

    public SaldoNoApuntadoException() {
        super("Todavía no has apuntado tu saldo del banco. Hazlo con POST /api/saldo-banco");
    }
}
