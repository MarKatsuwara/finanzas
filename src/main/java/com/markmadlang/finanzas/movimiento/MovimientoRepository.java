package com.markmadlang.finanzas.movimiento;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio: el "almacén" de movimientos.
 * Solo con extender JpaRepository, Spring nos regala save, findById, findAll, delete...
 *
 * Casi todos los métodos piden el usuarioId: así cada usuario SOLO ve sus movimientos.
 */
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    /**
     * Spring entiende el nombre del método y genera la consulta SQL él solo.
     * @EntityGraph trae también la categoría en la MISMA consulta, en vez de hacer
     * una consulta extra por cada movimiento (el famoso problema "N+1").
     */
    @EntityGraph(attributePaths = "categoria")
    List<Movimiento> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);

    /** Un movimiento, pero solo si es de ese usuario. Si es de otro, es como si no existiera. */
    Optional<Movimiento> findByIdAndUsuarioId(Long id, Long usuarioId);

    /** Los movimientos de un usuario entre dos fechas (las dos incluidas), del más antiguo al más nuevo. */
    List<Movimiento> findByUsuarioIdAndFechaBetweenOrderByFechaAscIdAsc(Long usuarioId, LocalDate desde, LocalDate hasta);

    /** ¿Hay algún movimiento que use esta categoría? (las categorías ya son de un solo usuario) */
    boolean existsByCategoriaId(Long categoriaId);

    /** Cuántos tiene un usuario (para el límite de Limites). */
    long countByUsuarioId(Long usuarioId);

    /**
     * Suma los importes de un usuario entre dos fechas, agrupando por tipo (INGRESO/GASTO) y por categoría.
     * Es como este SQL:
     *   SELECT m.tipo, c.id, c.nombre, SUM(m.importe)
     *   FROM movimientos m LEFT JOIN categorias c ON c.id = m.categoria_id
     *   WHERE m.usuario_id = :usuarioId AND m.fecha BETWEEN :desde AND :hasta
     *   GROUP BY m.tipo, c.id, c.nombre
     * Las sumas las hace la base de datos, que es muy rápida para eso.
     */
    @Query("""
            SELECT new com.markmadlang.finanzas.movimiento.TotalPorCategoria(m.tipo, c.id, c.nombre, SUM(m.importe))
            FROM Movimiento m LEFT JOIN m.categoria c
            WHERE m.usuarioId = :usuarioId AND m.fecha BETWEEN :desde AND :hasta
            GROUP BY m.tipo, c.id, c.nombre
            """)
    List<TotalPorCategoria> totalesPorCategoria(@Param("usuarioId") Long usuarioId,
                                                @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Los movimientos "sin dueño" (de antes de que hubiera usuarios) pasan a ser de este usuario. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Movimiento m SET m.usuarioId = :usuarioId WHERE m.usuarioId IS NULL")
    int adoptarLosSinDueno(@Param("usuarioId") Long usuarioId);

    /** Borra todos los de un usuario (cuando borra su cuenta). */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Movimiento m WHERE m.usuarioId = :usuarioId")
    int borrarTodosLosDe(@Param("usuarioId") Long usuarioId);
}
