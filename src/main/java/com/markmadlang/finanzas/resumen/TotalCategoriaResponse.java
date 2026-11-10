package com.markmadlang.finanzas.resumen;

import java.math.BigDecimal;

/** Cuánto se ha ingresado o gastado en una categoría. categoriaId es null en "Sin categoría". */
public record TotalCategoriaResponse(Long categoriaId, String categoria, BigDecimal total) {
}
