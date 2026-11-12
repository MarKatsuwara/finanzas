package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.Limites;
import com.markmadlang.finanzas.movimiento.Movimiento;
import com.markmadlang.finanzas.movimiento.MovimientoRepository;
import com.markmadlang.finanzas.movimiento.MovimientoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Apunta SOLOS como movimientos los gastos fijos que ya han llegado a su día.
 *
 * Se llama cada vez que se abre la app (no a una hora fija: el servidor gratis se duerme).
 * Cada fijo tiene una "marca" (apuntadoHasta): se apuntan las fechas que tocaban desde el día
 * siguiente a la marca hasta hoy, y la marca se mueve a hoy. Por eso nada se apunta dos veces.
 */
@Service
public class ApuntadorDeFijos {

    private final PagoRecurrenteRepository pagoRepository;
    private final MovimientoRepository movimientoRepository;
    private final Limites limites;
    private final Clock reloj;

    public ApuntadorDeFijos(PagoRecurrenteRepository pagoRepository, MovimientoRepository movimientoRepository,
                            Limites limites, Clock reloj) {
        this.pagoRepository = pagoRepository;
        this.movimientoRepository = movimientoRepository;
        this.limites = limites;
        this.reloj = reloj;
    }

    /** Apunta lo pendiente de un usuario y devuelve los movimientos nuevos (de más antiguo a más nuevo). */
    @Transactional
    public List<MovimientoResponse> apuntarPendientes(Long usuarioId) {
        LocalDate hoy = LocalDate.now(reloj);
        List<Movimiento> nuevos = new ArrayList<>();

        // Bloqueados hasta que acabe esta transacción (ver el repositorio)
        for (PagoRecurrente pago : pagoRepository.findByUsuarioIdAndActivoTrueAndApuntarSoloTrueOrderByIdAsc(usuarioId)) {
            for (LocalDate fecha : pago.fechasPendientesHasta(hoy)) {
                nuevos.add(Movimiento.automatico(usuarioId, pago.getTipo(), pago.getImporte(), fecha,
                        pago.getDescripcion(), pago.getCategoria(), pago.getId()));
            }
            pago.marcarApuntadoHasta(hoy); // JPA lo guarda solo al acabar
        }

        if (nuevos.isEmpty()) {
            return List.of();
        }
        // El mismo límite que al apuntar a mano (si se pasa: 409 y no se apunta nada, ni se mueve ninguna marca)
        limites.comprobarMovimientos(movimientoRepository.countByUsuarioId(usuarioId) + nuevos.size() - 1);

        return movimientoRepository.saveAll(nuevos).stream()
                .sorted(Comparator.comparing(Movimiento::getFecha).thenComparing(Movimiento::getDescripcion))
                .map(MovimientoResponse::desde)
                .toList();
    }
}
