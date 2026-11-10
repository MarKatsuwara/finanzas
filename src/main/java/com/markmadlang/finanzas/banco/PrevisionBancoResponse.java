package com.markmadlang.finanzas.banco;

import com.markmadlang.finanzas.movimiento.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Cómo irá tu saldo del banco día a día en los próximos días. */
public record PrevisionBancoResponse(
        LocalDate hoy,
        BigDecimal saldoHoy,
        LocalDate hasta,
        List<DiaPrevisto> dias,          // solo los días en los que pasa algo
        BigDecimal saldoFinal,           // con lo que acabarás el periodo
        BigDecimal saldoMinimo,          // lo más bajo que llegará a estar
        LocalDate primerDiaEnNegativo,   // null si nunca baja de 0
        boolean numerosRojos,
        String mensaje
) {

    /** Un día con movimientos y cómo queda el saldo al acabar ese día. */
    public record DiaPrevisto(LocalDate fecha, List<MovimientoPrevisto> movimientos, BigDecimal saldoAlFinal) {
    }

    /**
     * Algo que va a entrar o salir.
     * origen = "PAGO_RECURRENTE" (alquiler, Netflix...) o "MOVIMIENTO" (uno que apuntaste con fecha futura).
     */
    public record MovimientoPrevisto(String descripcion, TipoMovimiento tipo, BigDecimal importe, String origen) {
    }
}
