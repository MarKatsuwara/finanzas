package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.Limites;
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
    private final Limites limites;

    public PagoRecurrenteService(PagoRecurrenteRepository repository, CategoriaService categoriaService, Clock reloj,
                                 Limites limites) {
        this.repository = repository;
        this.categoriaService = categoriaService;
        this.reloj = reloj;
        this.limites = limites;
    }

    public List<PagoRecurrenteResponse> listar(Long usuarioId) {
        return repository.findByUsuarioIdOrderByDescripcionAscIdAsc(usuarioId).stream()
                .map(this::respuesta)
                .toList();
    }

    public PagoRecurrenteResponse obtener(Long usuarioId, Long id) {
        return respuesta(buscar(usuarioId, id));
    }

    @Transactional
    public PagoRecurrenteResponse crear(Long usuarioId, PagoRecurrenteRequest datos) {
        limites.comprobarGastosFijos(repository.countByUsuarioId(usuarioId));
        boolean activo = datos.activo() == null || datos.activo(); // si no lo dicen, activo
        PagoRecurrente nuevo = new PagoRecurrente(usuarioId, datos.descripcion().trim(), datos.tipo(), datos.importe(),
                datos.frecuencia(), datos.fechaInicio(), activo,
                categoriaService.buscarParaAsignar(usuarioId, datos.categoriaId()));
        return respuesta(repository.save(nuevo));
    }

    @Transactional
    public PagoRecurrenteResponse actualizar(Long usuarioId, Long id, PagoRecurrenteRequest datos) {
        PagoRecurrente existente = buscar(usuarioId, id);
        boolean activo = datos.activo() == null ? existente.isActivo() : datos.activo(); // si no lo dicen, igual
        existente.actualizar(datos.descripcion().trim(), datos.tipo(), datos.importe(),
                datos.frecuencia(), datos.fechaInicio(), activo,
                categoriaService.buscarParaAsignar(usuarioId, datos.categoriaId()));
        return respuesta(existente);
    }

    @Transactional
    public void borrar(Long usuarioId, Long id) {
        repository.delete(buscar(usuarioId, id));
    }

    /**
     * Los pagos de un usuario que tocan en los "dias" días que empiezan en "desde" (contando "desde"),
     * ordenados por fecha. Si dos caen el mismo día, por orden alfabético.
     * Ejemplo: desde=01/12 y dias=31 → del 01/12 al 31/12.
     */
    public List<ProximoPagoResponse> proximos(Long usuarioId, LocalDate desde, int dias) {
        if (dias < 1 || dias > MAX_DIAS) {
            throw new CampoNoValidoException("dias", "Los días deben estar entre 1 y " + MAX_DIAS);
        }
        LocalDate hasta = desde.plusDays(dias - 1); // -1 porque el primer día ya cuenta

        return repository.findByUsuarioIdAndActivoTrue(usuarioId).stream()
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

    /** Si no existe O es de otro usuario → 404. */
    private PagoRecurrente buscar(Long usuarioId, Long id) {
        return repository.findByIdAndUsuarioId(id, usuarioId).orElseThrow(() -> new PagoRecurrenteNoEncontradoException(id));
    }
}
