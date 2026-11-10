package com.markmadlang.finanzas.movimiento;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio: el "almacén" de movimientos.
 * Solo con extender JpaRepository, Spring nos regala save, findById, findAll, delete...
 */
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    /**
     * Spring entiende el nombre del método y genera la consulta SQL él solo.
     * @EntityGraph trae también la categoría en la MISMA consulta, en vez de hacer
     * una consulta extra por cada movimiento (el famoso problema "N+1").
     */
    @EntityGraph(attributePaths = "categoria")
    List<Movimiento> findAllByOrderByFechaDescIdDesc();

    /** ¿Hay algún movimiento que use esta categoría? */
    boolean existsByCategoriaId(Long categoriaId);
}
