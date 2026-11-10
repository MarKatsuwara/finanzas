package com.markmadlang.finanzas.movimiento;

import com.markmadlang.finanzas.categoria.CategoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio: aquí vive la lógica de la app (la "cocina").
 * El controlador no habla con la base de datos directamente; pasa siempre por aquí.
 */
@Service
@Transactional(readOnly = true)
public class MovimientoService {

    private final MovimientoRepository repository;
    private final CategoriaService categoriaService;

    // Spring nos pasa lo que necesitamos automáticamente (inyección de dependencias)
    public MovimientoService(MovimientoRepository repository, CategoriaService categoriaService) {
        this.repository = repository;
        this.categoriaService = categoriaService;
    }

    public List<MovimientoResponse> listar() {
        return repository.findAllByOrderByFechaDescIdDesc().stream()
                .map(MovimientoResponse::desde)
                .toList();
    }

    public MovimientoResponse obtener(Long id) {
        return MovimientoResponse.desde(buscar(id));
    }

    @Transactional
    public MovimientoResponse crear(MovimientoRequest datos) {
        Movimiento nuevo = new Movimiento(datos.tipo(), datos.importe(), datos.fecha(),
                datos.descripcion().trim(), categoriaService.buscarParaAsignar(datos.categoriaId()));
        return MovimientoResponse.desde(repository.save(nuevo));
    }

    @Transactional
    public MovimientoResponse actualizar(Long id, MovimientoRequest datos) {
        Movimiento existente = buscar(id);
        existente.actualizar(datos.tipo(), datos.importe(), datos.fecha(),
                datos.descripcion().trim(), categoriaService.buscarParaAsignar(datos.categoriaId()));
        // No hace falta save(): al acabar la transacción, JPA guarda los cambios solo
        return MovimientoResponse.desde(existente);
    }

    @Transactional
    public void borrar(Long id) {
        repository.delete(buscar(id));
    }

    private Movimiento buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new MovimientoNoEncontradoException(id));
    }
}
