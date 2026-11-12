package com.markmadlang.finanzas.pagorecurrente;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** El "almacén" de pagos recurrentes. Cada usuario solo ve los suyos. */
public interface PagoRecurrenteRepository extends JpaRepository<PagoRecurrente, Long> {

    /** Todos los de un usuario, ordenados por descripción, trayendo la categoría en la misma consulta. */
    @EntityGraph(attributePaths = "categoria")
    List<PagoRecurrente> findByUsuarioIdOrderByDescripcionAscIdAsc(Long usuarioId);

    /** Solo los activos (los dados de baja no cuentan para los próximos pagos). */
    @EntityGraph(attributePaths = "categoria")
    List<PagoRecurrente> findByUsuarioIdAndActivoTrue(Long usuarioId);

    /**
     * Los que se pueden apuntar solos (activos y con "Apuntar solo" encendido), BLOQUEADOS hasta acabar:
     * si la app se abre en dos pestañas a la vez, la segunda espera a la primera y ya no encuentra
     * nada pendiente, así nada se apunta dos veces. (Sin @EntityGraph: PostgreSQL no deja bloquear con LEFT JOIN.)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PagoRecurrente> findByUsuarioIdAndActivoTrueAndApuntarSoloTrueOrderByIdAsc(Long usuarioId);

    /** Un pago, pero solo si es de ese usuario. */
    Optional<PagoRecurrente> findByIdAndUsuarioId(Long id, Long usuarioId);

    /** ¿Hay algún pago recurrente que use esta categoría? */
    boolean existsByCategoriaId(Long categoriaId);

    /** Cuántos tiene un usuario (para el límite de Limites). */
    long countByUsuarioId(Long usuarioId);

    /** Los pagos "sin dueño" (de antes de que hubiera usuarios) pasan a ser de este usuario. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE PagoRecurrente p SET p.usuarioId = :usuarioId WHERE p.usuarioId IS NULL")
    int adoptarLosSinDueno(@Param("usuarioId") Long usuarioId);

    /** Borra todos los de un usuario (cuando borra su cuenta). */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM PagoRecurrente p WHERE p.usuarioId = :usuarioId")
    int borrarTodosLosDe(@Param("usuarioId") Long usuarioId);
}
