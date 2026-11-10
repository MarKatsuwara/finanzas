package com.markmadlang.finanzas.banco;

import com.markmadlang.finanzas.banco.PrevisionBancoResponse.DiaPrevisto;
import com.markmadlang.finanzas.banco.PrevisionBancoResponse.MovimientoPrevisto;
import com.markmadlang.finanzas.error.CampoNoValidoException;
import com.markmadlang.finanzas.movimiento.MovimientoRepository;
import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import com.markmadlang.finanzas.movimiento.TotalPorCategoria;
import com.markmadlang.finanzas.pagorecurrente.PagoRecurrenteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Lleva la cuenta del dinero que tienes en el banco. */
@Service
@Transactional(readOnly = true)
public class SaldoBancoService {

    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SaldoBancoRepository repository;
    private final MovimientoRepository movimientoRepository;
    private final PagoRecurrenteService pagoRecurrenteService;
    private final Clock reloj;

    public SaldoBancoService(SaldoBancoRepository repository, MovimientoRepository movimientoRepository,
                             PagoRecurrenteService pagoRecurrenteService, Clock reloj) {
        this.repository = repository;
        this.movimientoRepository = movimientoRepository;
        this.pagoRecurrenteService = pagoRecurrenteService;
        this.reloj = reloj;
    }

    @Transactional
    public SaldoApuntadoResponse apuntar(SaldoBancoRequest datos) {
        return SaldoApuntadoResponse.desde(repository.save(new SaldoBanco(datos.fecha(), datos.importe())));
    }

    public List<SaldoApuntadoResponse> historial() {
        return repository.findAllByOrderByFechaDescIdDesc().stream()
                .map(SaldoApuntadoResponse::desde)
                .toList();
    }

    /** ¿Cuánto tengo hoy? */
    public SaldoActualResponse saldoActual() {
        LocalDate hoy = LocalDate.now(reloj);
        SaldoBanco apuntado = repository.findFirstByFechaLessThanEqualOrderByFechaDescIdDesc(hoy)
                .orElseThrow(SaldoNoApuntadoException::new);

        // Los movimientos de ESE MISMO día ya están dentro del saldo que viste en el banco,
        // así que solo contamos desde el día siguiente hasta hoy.
        LocalDate desde = apuntado.getFecha().plusDays(1);
        BigDecimal ingresos = BigDecimal.ZERO.setScale(2);
        BigDecimal gastos = BigDecimal.ZERO.setScale(2);
        if (!desde.isAfter(hoy)) {
            for (TotalPorCategoria fila : movimientoRepository.totalesPorCategoria(desde, hoy)) {
                if (fila.tipo() == TipoMovimiento.INGRESO) {
                    ingresos = ingresos.add(fila.total());
                } else {
                    gastos = gastos.add(fila.total());
                }
            }
        }

        BigDecimal saldoHoy = apuntado.getImporte().add(ingresos).subtract(gastos);
        return new SaldoActualResponse(hoy, SaldoApuntadoResponse.desde(apuntado), ingresos, gastos, saldoHoy);
    }

    /** ¿Cómo irá mi saldo en los próximos "dias" días? (de mañana a hoy + dias) */
    public PrevisionBancoResponse prevision(int dias) {
        if (dias < 1 || dias > PagoRecurrenteService.MAX_DIAS) {
            throw new CampoNoValidoException("dias", "Los días deben estar entre 1 y " + PagoRecurrenteService.MAX_DIAS);
        }
        SaldoActualResponse actual = saldoActual();
        LocalDate manana = actual.hoy().plusDays(1);
        LocalDate hasta = actual.hoy().plusDays(dias);

        // 1. Juntamos todo lo que va a pasar, agrupado por día.
        //    TreeMap = un mapa que mantiene las fechas ordenadas solas.
        Map<LocalDate, List<MovimientoPrevisto>> porDia = new TreeMap<>();

        // a) Pagos recurrentes (alquiler, Netflix, nómina...)
        pagoRecurrenteService.proximos(manana, dias).forEach(pago ->
                porDia.computeIfAbsent(pago.fecha(), f -> new ArrayList<>())
                        .add(new MovimientoPrevisto(pago.descripcion(), pago.tipo(), pago.importe(), "PAGO_RECURRENTE")));

        // b) Movimientos que apuntaste con fecha futura
        movimientoRepository.findByFechaBetweenOrderByFechaAscIdAsc(manana, hasta).forEach(m ->
                porDia.computeIfAbsent(m.getFecha(), f -> new ArrayList<>())
                        .add(new MovimientoPrevisto(m.getDescripcion(), m.getTipo(), m.getImporte(), "MOVIMIENTO")));

        // 2. Vamos día a día sumando y restando, y apuntamos cómo acaba cada día.
        //    Miramos el saldo al FINAL del día: si la nómina y el alquiler caen el mismo día,
        //    da igual cuál llegue antes.
        BigDecimal saldo = actual.saldoHoy();
        BigDecimal minimo = saldo;
        LocalDate primerDiaEnNegativo = saldo.signum() < 0 ? actual.hoy() : null;
        List<DiaPrevisto> diasPrevistos = new ArrayList<>();

        for (Map.Entry<LocalDate, List<MovimientoPrevisto>> dia : porDia.entrySet()) {
            for (MovimientoPrevisto m : dia.getValue()) {
                saldo = m.tipo() == TipoMovimiento.INGRESO ? saldo.add(m.importe()) : saldo.subtract(m.importe());
            }
            diasPrevistos.add(new DiaPrevisto(dia.getKey(), dia.getValue(), saldo));

            if (saldo.compareTo(minimo) < 0) {
                minimo = saldo;
            }
            if (saldo.signum() < 0 && primerDiaEnNegativo == null) {
                primerDiaEnNegativo = dia.getKey();
            }
        }

        boolean numerosRojos = primerDiaEnNegativo != null;
        return new PrevisionBancoResponse(actual.hoy(), actual.saldoHoy(), hasta, diasPrevistos, saldo, minimo,
                primerDiaEnNegativo, numerosRojos, mensaje(primerDiaEnNegativo, diasPrevistos, actual, minimo, dias));
    }

    private String mensaje(LocalDate primerDiaEnNegativo, List<DiaPrevisto> dias, SaldoActualResponse actual,
                           BigDecimal minimo, int numeroDeDias) {
        if (primerDiaEnNegativo == null) {
            return "Vas bien: en los próximos " + numeroDeDias + " días tu saldo no bajará de "
                    + minimo.toPlainString() + " €";
        }
        if (primerDiaEnNegativo.equals(actual.hoy())) {
            return "Cuidado: ya estás en negativo (" + actual.saldoHoy().toPlainString() + " €)";
        }
        BigDecimal saldoEseDia = dias.stream()
                .filter(d -> d.fecha().equals(primerDiaEnNegativo))
                .findFirst().orElseThrow()
                .saldoAlFinal();
        return "Cuidado: el " + primerDiaEnNegativo.format(DD_MM_YYYY) + " te quedarás en "
                + saldoEseDia.toPlainString() + " €";
    }
}
