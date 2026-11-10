package com.markmadlang.finanzas.pagorecurrente;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** El "almacén" de pagos recurrentes. */
public interface PagoRecurrenteRepository extends JpaRepository<PagoRecurrente, Long> {

    /** Todos, ordenados por descripción, trayendo la categoría en la misma consulta. */
    @EntityGraph(attributePaths = "categoria")
    List<PagoRecurrente> findAllByOrderByDescripcionAscIdAsc();

    /** Solo los activos (los dados de baja no cuentan para los próximos pagos). */
    @EntityGraph(attributePaths = "categoria")
    List<PagoRecurrente> findByActivoTrue();

    /** ¿Hay algún pago recurrente que use esta categoría? */
    boolean existsByCategoriaId(Long categoriaId);
}
