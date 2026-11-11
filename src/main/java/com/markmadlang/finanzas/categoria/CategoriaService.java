package com.markmadlang.finanzas.categoria;

import com.markmadlang.finanzas.error.CampoNoValidoException;
import com.markmadlang.finanzas.error.ConflictoException;
import com.markmadlang.finanzas.movimiento.MovimientoRepository;
import com.markmadlang.finanzas.pagorecurrente.PagoRecurrenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** La lógica de las categorías. Cada usuario tiene las suyas. */
@Service
@Transactional(readOnly = true)
public class CategoriaService {

    /** Las categorías con las que empieza cada usuario nuevo (las mismas que creaba la V2). */
    public static final List<String> CATEGORIAS_DE_SERIE =
            List.of("Comida", "Vivienda", "Transporte", "Ocio", "Suscripciones", "Nómina", "Otros");

    private final CategoriaRepository repository;
    private final MovimientoRepository movimientoRepository;
    private final PagoRecurrenteRepository pagoRecurrenteRepository;

    public CategoriaService(CategoriaRepository repository, MovimientoRepository movimientoRepository,
                            PagoRecurrenteRepository pagoRecurrenteRepository) {
        this.repository = repository;
        this.movimientoRepository = movimientoRepository;
        this.pagoRecurrenteRepository = pagoRecurrenteRepository;
    }

    public List<CategoriaResponse> listar(Long usuarioId) {
        return repository.findByUsuarioIdOrderByNombreAsc(usuarioId).stream()
                .map(CategoriaResponse::desde)
                .toList();
    }

    public CategoriaResponse obtener(Long usuarioId, Long id) {
        return CategoriaResponse.desde(buscar(usuarioId, id));
    }

    @Transactional
    public CategoriaResponse crear(Long usuarioId, CategoriaRequest datos) {
        String nombre = datos.nombre().trim();
        if (repository.existsByUsuarioIdAndNombreIgnoreCase(usuarioId, nombre)) {
            throw new ConflictoException("Ya existe una categoría llamada " + nombre);
        }
        return CategoriaResponse.desde(repository.save(new Categoria(usuarioId, nombre)));
    }

    @Transactional
    public CategoriaResponse actualizar(Long usuarioId, Long id, CategoriaRequest datos) {
        Categoria categoria = buscar(usuarioId, id);
        String nombre = datos.nombre().trim();
        if (repository.existsByUsuarioIdAndNombreIgnoreCaseAndIdNot(usuarioId, nombre, id)) {
            throw new ConflictoException("Ya existe una categoría llamada " + nombre);
        }
        categoria.cambiarNombre(nombre);
        return CategoriaResponse.desde(categoria);
    }

    @Transactional
    public void borrar(Long usuarioId, Long id) {
        Categoria categoria = buscar(usuarioId, id);
        // No dejamos movimientos "huérfanos": si alguno la usa, no se puede borrar
        if (movimientoRepository.existsByCategoriaId(id)) {
            throw new ConflictoException("No se puede borrar la categoría " + categoria.getNombre()
                    + " porque hay movimientos que la usan");
        }
        if (pagoRecurrenteRepository.existsByCategoriaId(id)) {
            throw new ConflictoException("No se puede borrar la categoría " + categoria.getNombre()
                    + " porque hay pagos recurrentes que la usan");
        }
        repository.delete(categoria);
    }

    /** Si el usuario todavía no tiene ninguna categoría, le creamos las de serie. */
    @Transactional
    public void crearLasDeSerieSiNoTiene(Long usuarioId) {
        if (repository.countByUsuarioId(usuarioId) == 0) {
            CATEGORIAS_DE_SERIE.forEach(nombre -> repository.save(new Categoria(usuarioId, nombre)));
        }
    }

    /**
     * Para cuando un movimiento o un pago quiere ponerse una categoría.
     * Sin categoría → null. Con una que no existe (o que es de otro usuario) → error 400 en "categoriaId".
     */
    public Categoria buscarParaAsignar(Long usuarioId, Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return repository.findByIdAndUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new CampoNoValidoException("categoriaId",
                        "No existe ninguna categoría con id " + categoriaId));
    }

    /** Si no existe O es de otro usuario → 404. */
    private Categoria buscar(Long usuarioId, Long id) {
        return repository.findByIdAndUsuarioId(id, usuarioId).orElseThrow(() -> new CategoriaNoEncontradaException(id));
    }
}
