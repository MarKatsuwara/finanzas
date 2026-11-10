package com.markmadlang.finanzas.categoria;

/** Lo que devolvemos de una categoría. */
public record CategoriaResponse(Long id, String nombre) {

    /** Convierte una entidad en respuesta. Si no hay categoría, devuelve null. */
    public static CategoriaResponse desde(Categoria c) {
        return c == null ? null : new CategoriaResponse(c.getId(), c.getNombre());
    }
}
