package com.markmadlang.finanzas.banco;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** El "almacén" de saldos del banco. Cada usuario solo ve los suyos. */
public interface SaldoBancoRepository extends JpaRepository<SaldoBanco, Long> {

    /**
     * El saldo más reciente de un usuario hasta una fecha (la de hoy).
     * Si hay dos el mismo día, gana el último que se apuntó (el id más alto).
     */
    Optional<SaldoBanco> findFirstByUsuarioIdAndFechaLessThanEqualOrderByFechaDescIdDesc(Long usuarioId, LocalDate hasta);

    /** Todos los de un usuario, del más reciente al más antiguo. */
    List<SaldoBanco> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);

    /** Cuántos tiene un usuario (para el límite de Limites). */
    long countByUsuarioId(Long usuarioId);

    /** Los saldos "sin dueño" (de antes de que hubiera usuarios) pasan a ser de este usuario. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE SaldoBanco s SET s.usuarioId = :usuarioId WHERE s.usuarioId IS NULL")
    int adoptarLosSinDueno(@Param("usuarioId") Long usuarioId);

    /** Borra todos los de un usuario (cuando borra su cuenta). */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM SaldoBanco s WHERE s.usuarioId = :usuarioId")
    int borrarTodosLosDe(@Param("usuarioId") Long usuarioId);
}
