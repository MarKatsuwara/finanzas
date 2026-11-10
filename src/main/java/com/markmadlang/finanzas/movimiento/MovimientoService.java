package com.markmadlang.finanzas.movimiento;

import com.markmadlang.finanzas.categoria.Categoria;
import com.markmadlang.finanzas.categoria.CategoriaRepository;
import com.markmadlang.finanzas.error.CampoNoValidoException;
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
    private final CategoriaRepository categoriaRepository;

    // Spring nos pasa los repositorios automáticamente (inyección de dependencias)
    public MovimientoService(MovimientoRepository repository, CategoriaRepository categoriaRepository) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
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
                datos.descripcion().trim(), buscarCategoria(datos.categoriaId()));
        return MovimientoResponse.desde(repository.save(nuevo));
    }

    @Transactional
    public MovimientoResponse actualizar(Long id, MovimientoRequest datos) {
        Movimiento existente = buscar(id);
        existente.actualizar(datos.tipo(), datos.importe(), datos.fecha(),
                datos.descripcion().trim(), buscarCategoria(datos.categoriaId()));
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

    /** Sin categoría → null. Con una que no existe → error 400. */
    private Categoria buscarCategoria(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new CampoNoValidoException("categoriaId",
                        "No existe ninguna categoría con id " + categoriaId));
    }
}
