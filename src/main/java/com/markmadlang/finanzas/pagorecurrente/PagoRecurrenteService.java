package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.categoria.CategoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** La lógica de los pagos recurrentes. */
@Service
@Transactional(readOnly = true)
public class PagoRecurrenteService {

    private final PagoRecurrenteRepository repository;
    private final CategoriaService categoriaService;

    public PagoRecurrenteService(PagoRecurrenteRepository repository, CategoriaService categoriaService) {
        this.repository = repository;
        this.categoriaService = categoriaService;
    }

    public List<PagoRecurrenteResponse> listar() {
        return repository.findAllByOrderByDescripcionAscIdAsc().stream()
                .map(PagoRecurrenteResponse::desde)
                .toList();
    }

    public PagoRecurrenteResponse obtener(Long id) {
        return PagoRecurrenteResponse.desde(buscar(id));
    }

    @Transactional
    public PagoRecurrenteResponse crear(PagoRecurrenteRequest datos) {
        boolean activo = datos.activo() == null || datos.activo(); // si no lo dicen, activo
        PagoRecurrente nuevo = new PagoRecurrente(datos.descripcion().trim(), datos.tipo(), datos.importe(),
                datos.frecuencia(), datos.fechaInicio(), activo,
                categoriaService.buscarParaAsignar(datos.categoriaId()));
        return PagoRecurrenteResponse.desde(repository.save(nuevo));
    }

    @Transactional
    public PagoRecurrenteResponse actualizar(Long id, PagoRecurrenteRequest datos) {
        PagoRecurrente existente = buscar(id);
        boolean activo = datos.activo() == null ? existente.isActivo() : datos.activo(); // si no lo dicen, igual
        existente.actualizar(datos.descripcion().trim(), datos.tipo(), datos.importe(),
                datos.frecuencia(), datos.fechaInicio(), activo,
                categoriaService.buscarParaAsignar(datos.categoriaId()));
        return PagoRecurrenteResponse.desde(existente);
    }

    @Transactional
    public void borrar(Long id) {
        repository.delete(buscar(id));
    }

    private PagoRecurrente buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new PagoRecurrenteNoEncontradoException(id));
    }
}
