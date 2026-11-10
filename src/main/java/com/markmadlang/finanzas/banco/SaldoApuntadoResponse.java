package com.markmadlang.finanzas.banco;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un saldo que apuntaste: "el 10/11 tenías 400 €". */
public record SaldoApuntadoResponse(Long id, LocalDate fecha, BigDecimal importe) {

    static SaldoApuntadoResponse desde(SaldoBanco s) {
        return new SaldoApuntadoResponse(s.getId(), s.getFecha(), s.getImporte());
    }
}
