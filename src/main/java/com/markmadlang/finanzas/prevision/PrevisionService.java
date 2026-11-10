package com.markmadlang.finanzas.prevision;

import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import com.markmadlang.finanzas.pagorecurrente.PagoRecurrenteService;
import com.markmadlang.finanzas.pagorecurrente.ProximoPagoResponse;
import com.markmadlang.finanzas.resumen.ResumenMensualResponse;
import com.markmadlang.finanzas.resumen.ResumenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Calcula cómo vas a acabar el mes juntando dos cosas que ya teníamos:
 *  - el resumen del mes (lo que ya has apuntado)        → ResumenService
 *  - los próximos pagos (lo que todavía falta)           → PagoRecurrenteService
 */
@Service
@Transactional(readOnly = true)
public class PrevisionService {

    private final ResumenService resumenService;
    private final PagoRecurrenteService pagoRecurrenteService;
    private final Clock reloj;

    public PrevisionService(ResumenService resumenService, PagoRecurrenteService pagoRecurrenteService, Clock reloj) {
        this.resumenService = resumenService;
        this.pagoRecurrenteService = pagoRecurrenteService;
        this.reloj = reloj;
    }

    /** Previsión del mes actual, según el reloj de la app. */
    public PrevisionResponse prevision() {
        return prevision(LocalDate.now(reloj));
    }

    /** Previsión del mes de "hoy", como si hoy fuera esa fecha. */
    public PrevisionResponse prevision(LocalDate hoy) {
        YearMonth mes = YearMonth.from(hoy);

        // 1. Lo que ya has apuntado este mes
        ResumenMensualResponse resumen = resumenService.resumenDelMes(mes);

        // 2. Lo que falta: pagos recurrentes de MAÑANA a fin de mes.
        //    (Lo de hoy no cuenta: se supone que ya lo has apuntado como movimiento.)
        List<ProximoPagoResponse> pendientes = pagosPendientes(hoy.plusDays(1), mes.atEndOfMonth());
        BigDecimal ingresosPendientes = sumar(pendientes, TipoMovimiento.INGRESO);
        BigDecimal gastosPendientes = sumar(pendientes, TipoMovimiento.GASTO);

        // 3. La cuenta final
        BigDecimal saldoPrevisto = resumen.totalIngresos().add(ingresosPendientes)
                .subtract(resumen.totalGastos())
                .subtract(gastosPendientes);
        boolean numerosRojos = saldoPrevisto.signum() < 0; // signum: -1 si es negativo, 0 si es cero, 1 si es positivo

        return new PrevisionResponse(mes, hoy,
                resumen.totalIngresos(), resumen.totalGastos(),
                ingresosPendientes, gastosPendientes,
                saldoPrevisto, numerosRojos, mensaje(saldoPrevisto), pendientes);
    }

    private List<ProximoPagoResponse> pagosPendientes(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            return List.of(); // hoy es el último día del mes: ya no queda nada
        }
        int dias = (int) ChronoUnit.DAYS.between(desde, hasta) + 1; // +1 porque los dos días cuentan
        return pagoRecurrenteService.proximos(desde, dias);
    }

    private BigDecimal sumar(List<ProximoPagoResponse> pagos, TipoMovimiento tipo) {
        return pagos.stream()
                .filter(pago -> pago.tipo() == tipo)
                .map(ProximoPagoResponse::importe)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
    }

    private String mensaje(BigDecimal saldoPrevisto) {
        if (saldoPrevisto.signum() < 0) {
            return "Cuidado: si sigues así, este mes gastarás " + saldoPrevisto.abs().toPlainString()
                    + " € más de lo que ingresas";
        }
        return "Vas bien: acabarás el mes con " + saldoPrevisto.toPlainString() + " € de margen";
    }
}
