package com.markmadlang.finanzas.resumen;

import com.markmadlang.finanzas.movimiento.MovimientoRepository;
import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import com.markmadlang.finanzas.movimiento.TotalPorCategoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/** Calcula los resúmenes a partir de los movimientos apuntados. */
@Service
@Transactional(readOnly = true)
public class ResumenService {

    private static final String SIN_CATEGORIA = "Sin categoría";

    private final MovimientoRepository movimientoRepository;

    public ResumenService(MovimientoRepository movimientoRepository) {
        this.movimientoRepository = movimientoRepository;
    }

    public ResumenMensualResponse resumenDelMes(YearMonth mes) {
        // Del día 1 al último día del mes (28, 29, 30 o 31: YearMonth lo sabe)
        List<TotalPorCategoria> filas = movimientoRepository.totalesPorCategoria(mes.atDay(1), mes.atEndOfMonth());

        List<TotalCategoriaResponse> ingresos = porCategoria(filas, TipoMovimiento.INGRESO);
        List<TotalCategoriaResponse> gastos = porCategoria(filas, TipoMovimiento.GASTO);

        BigDecimal totalIngresos = sumar(ingresos);
        BigDecimal totalGastos = sumar(gastos);

        return new ResumenMensualResponse(mes, totalIngresos, totalGastos,
                totalIngresos.subtract(totalGastos), ingresos, gastos);
    }

    /** Se queda con las filas de un tipo y las ordena de mayor a menor importe. */
    private List<TotalCategoriaResponse> porCategoria(List<TotalPorCategoria> filas, TipoMovimiento tipo) {
        return filas.stream()
                .filter(fila -> fila.tipo() == tipo)
                .map(fila -> new TotalCategoriaResponse(
                        fila.categoriaId(),
                        fila.categoriaNombre() != null ? fila.categoriaNombre() : SIN_CATEGORIA,
                        dosDecimales(fila.total())))
                .sorted(Comparator.comparing(TotalCategoriaResponse::total).reversed()
                        .thenComparing(TotalCategoriaResponse::categoria))
                .toList();
    }

    private BigDecimal sumar(List<TotalCategoriaResponse> totales) {
        BigDecimal suma = totales.stream()
                .map(TotalCategoriaResponse::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return dosDecimales(suma);
    }

    /** Para que siempre se vea "0.00" y no "0". */
    private BigDecimal dosDecimales(BigDecimal numero) {
        return numero.setScale(2);
    }
}
