package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.categoria.CategoriaService;
import com.markmadlang.finanzas.error.CampoNoValidoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** La lógica de los pagos recurrentes. */
@Service
@Transactional(readOnly = true)
public class PagoRecurrenteService {

    /** Como mucho se puede mirar un año (y un día, por los bisiestos) hacia delante. */
    public static final int MAX_DIAS = 366;

    private final PagoRecurrenteRepository repository;
    private final CategoriaService categoriaService;
    private final Clock reloj;

    public PagoRecurrenteService(PagoRecurrenteRepository repository, CategoriaService categoriaService, Clock reloj) {
        this.repository = repository;
        this.categoriaService = categoriaService;
        this.reloj = reloj;
    }

    public List<PagoRecurrenteResponse> listar() {
        return repository.findAllByOrderByDescripcionAscIdAsc().stream()
                .map(this::respuesta)
                .toList();
    }

    public PagoRecurrenteResponse obtener(Long id) {
        return respuesta(buscar(id));
    }

    @Transactional
    public PagoRecurrenteResponse crear(PagoRecurrenteRequest datos) {
        boolean activo = datos.activo() == null || datos.activo(); // si no lo dicen, activo
        PagoRecurrente nuevo = new PagoRecurrente(datos.descripcion().trim(), datos.tipo(), datos.importe(),
                datos.frecuencia(), datos.fechaInicio(), activo,
                categoriaService.buscarParaAsignar(datos.categoriaId()));
        return respuesta(repository.save(nuevo));
    }

    @Transactional
    public PagoRecurrenteResponse actualizar(Long id, PagoRecurrenteRequest datos) {
        PagoRecurrente existente = buscar(id);
        boolean activo = datos.activo() == null ? existente.isActivo() : datos.activo(); // si no lo dicen, igual
        existente.actualizar(datos.descripcion().trim(), datos.tipo(), datos.importe(),
                datos.frecuencia(), datos.fechaInicio(), activo,
                categoriaService.buscarParaAsignar(datos.categoriaId()));
        return respuesta(existente);
    }

    @Transactional
    public void borrar(Long id) {
        repository.delete(buscar(id));
    }

    /**
     * Los pagos que tocan en los "dias" días que empiezan en "desde" (contando "desde"),
     * ordenados por fecha. Si dos caen el mismo día, por orden alfabético.
     * Ejemplo: desde=01/12 y dias=31 → del 01/12 al 31/12.
     */
    public List<ProximoPagoResponse> proximos(LocalDate desde, int dias) {
        if (dias < 1 || dias > MAX_DIAS) {
            throw new CampoNoValidoException("dias", "Los días deben estar entre 1 y " + MAX_DIAS);
        }
        LocalDate hasta = desde.plusDays(dias - 1); // -1 porque el primer día ya cuenta

        return repository.findByActivoTrue().stream()
                // cada pago se convierte en tantos "próximos pagos" como fechas le toquen
                .flatMap(pago -> pago.fechasEntre(desde, hasta).stream()
                        .map(fecha -> ProximoPagoResponse.desde(pago, fecha)))
                .sorted(Comparator.comparing(ProximoPagoResponse::fecha)
                        .thenComparing(ProximoPagoResponse::descripcion))
                .toList();
    }

    /** Convierte un pago en respuesta, calculando su próxima fecha a partir de hoy. */
    private PagoRecurrenteResponse respuesta(PagoRecurrente pago) {
        return PagoRecurrenteResponse.desde(pago, LocalDate.now(reloj));
    }

    private PagoRecurrente buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new PagoRecurrenteNoEncontradoException(id));
    }
}
