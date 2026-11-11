package com.markmadlang.finanzas.movimiento;

import com.markmadlang.finanzas.categoria.CategoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio: aquí vive la lógica de la app (la "cocina").
 * El controlador no habla con la base de datos directamente; pasa siempre por aquí.
 *
 * Todos los métodos reciben el usuarioId: el cocinero solo cocina con los ingredientes de ese usuario.
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

    public List<MovimientoResponse> listar(Long usuarioId) {
        return repository.findByUsuarioIdOrderByFechaDescIdDesc(usuarioId).stream()
                .map(MovimientoResponse::desde)
                .toList();
    }

    public MovimientoResponse obtener(Long usuarioId, Long id) {
        return MovimientoResponse.desde(buscar(usuarioId, id));
    }

    @Transactional
    public MovimientoResponse crear(Long usuarioId, MovimientoRequest datos) {
        Movimiento nuevo = new Movimiento(usuarioId, datos.tipo(), datos.importe(), datos.fecha(),
                datos.descripcion().trim(), categoriaService.buscarParaAsignar(usuarioId, datos.categoriaId()));
        return MovimientoResponse.desde(repository.save(nuevo));
    }

    @Transactional
    public MovimientoResponse actualizar(Long usuarioId, Long id, MovimientoRequest datos) {
        Movimiento existente = buscar(usuarioId, id);
        existente.actualizar(datos.tipo(), datos.importe(), datos.fecha(),
                datos.descripcion().trim(), categoriaService.buscarParaAsignar(usuarioId, datos.categoriaId()));
        // No hace falta save(): al acabar la transacción, JPA guarda los cambios solo
        return MovimientoResponse.desde(existente);
    }

    @Transactional
    public void borrar(Long usuarioId, Long id) {
        repository.delete(buscar(usuarioId, id));
    }

    /** Si el movimiento no existe O es de otro usuario → 404 (así ni sabe que existe). */
    private Movimiento buscar(Long usuarioId, Long id) {
        return repository.findByIdAndUsuarioId(id, usuarioId).orElseThrow(() -> new MovimientoNoEncontradoException(id));
    }
}
