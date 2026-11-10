package com.markmadlang.finanzas.categoria;

import com.markmadlang.finanzas.error.NoEncontradoException;

/** Se lanza cuando alguien pide una categoría que no existe. Acaba convertido en un 404. */
public class CategoriaNoEncontradaException extends NoEncontradoException {

    public CategoriaNoEncontradaException(Long id) {
        super("No existe ninguna categoría con id " + id);
    }
}
