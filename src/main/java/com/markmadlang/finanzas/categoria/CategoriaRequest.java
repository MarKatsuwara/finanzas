package com.markmadlang.finanzas.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lo que nos envían para crear o renombrar una categoría. */
public record CategoriaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre no puede pasar de 50 caracteres")
        String nombre
) {
}
