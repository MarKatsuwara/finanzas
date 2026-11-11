package com.markmadlang.finanzas.usuario;

import com.markmadlang.finanzas.error.NoEncontradoException;

public class UsuarioNoEncontradoException extends NoEncontradoException {

    public UsuarioNoEncontradoException(Long id) {
        super("No existe ningún usuario con id " + id);
    }
}
