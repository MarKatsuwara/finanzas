package com.markmadlang.finanzas.movimiento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio: el "almacén" de movimientos.
 * Solo con extender JpaRepository, Spring nos regala save, findById, findAll, delete...
 */
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    /** Spring entiende el nombre del método y genera la consulta SQL él solo. */
    List<Movimiento> findAllByOrderByFechaDescIdDesc();
}
