package com.markmadlang.finanzas.categoria;

import com.markmadlang.finanzas.error.ConflictoException;
import com.markmadlang.finanzas.movimiento.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** La lógica de las categorías. */
@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository repository;
    private final MovimientoRepository movimientoRepository;

    public CategoriaService(CategoriaRepository repository, MovimientoRepository movimientoRepository) {
        this.repository = repository;
        this.movimientoRepository = movimientoRepository;
    }

    public List<CategoriaResponse> listar() {
        return repository.findAllByOrderByNombreAsc().stream()
                .map(CategoriaResponse::desde)
                .toList();
    }

    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.desde(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest datos) {
        String nombre = datos.nombre().trim();
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe una categoría llamada " + nombre);
        }
        return CategoriaResponse.desde(repository.save(new Categoria(nombre)));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest datos) {
        Categoria categoria = buscar(id);
        String nombre = datos.nombre().trim();
        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictoException("Ya existe una categoría llamada " + nombre);
        }
        categoria.cambiarNombre(nombre);
        return CategoriaResponse.desde(categoria);
    }

    @Transactional
    public void borrar(Long id) {
        Categoria categoria = buscar(id);
        // No dejamos movimientos "huérfanos": si alguno la usa, no se puede borrar
        if (movimientoRepository.existsByCategoriaId(id)) {
            throw new ConflictoException("No se puede borrar la categoría " + categoria.getNombre()
                    + " porque hay movimientos que la usan");
        }
        repository.delete(categoria);
    }

    private Categoria buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new CategoriaNoEncontradaException(id));
    }
}
