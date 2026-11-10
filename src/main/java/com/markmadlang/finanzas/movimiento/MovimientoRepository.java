package com.markmadlang.finanzas.movimiento;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    /**
     * Suma los importes entre dos fechas, agrupando por tipo (INGRESO/GASTO) y por categoría.
     * Es como este SQL:
     *   SELECT m.tipo, c.id, c.nombre, SUM(m.importe)
     *   FROM movimientos m LEFT JOIN categorias c ON c.id = m.categoria_id
     *   WHERE m.fecha BETWEEN :desde AND :hasta
     *   GROUP BY m.tipo, c.id, c.nombre
     * Las sumas las hace la base de datos, que es muy rápida para eso.
     */
    @Query("""
            SELECT new com.markmadlang.finanzas.movimiento.TotalPorCategoria(m.tipo, c.id, c.nombre, SUM(m.importe))
            FROM Movimiento m LEFT JOIN m.categoria c
            WHERE m.fecha BETWEEN :desde AND :hasta
            GROUP BY m.tipo, c.id, c.nombre
            """)
    List<TotalPorCategoria> totalesPorCategoria(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
