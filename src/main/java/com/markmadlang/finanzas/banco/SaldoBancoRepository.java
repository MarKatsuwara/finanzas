package com.markmadlang.finanzas.banco;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** El "almacén" de saldos del banco. */
public interface SaldoBancoRepository extends JpaRepository<SaldoBanco, Long> {

    /**
     * El saldo más reciente hasta una fecha (la de hoy).
     * Si hay dos el mismo día, gana el último que se apuntó (el id más alto).
     */
    Optional<SaldoBanco> findFirstByFechaLessThanEqualOrderByFechaDescIdDesc(LocalDate hasta);

    /** Todos, del más reciente al más antiguo. */
    List<SaldoBanco> findAllByOrderByFechaDescIdDesc();
}
