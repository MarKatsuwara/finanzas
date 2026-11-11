package com.markmadlang.finanzas.usuario;

import com.markmadlang.finanzas.banco.SaldoBancoRepository;
import com.markmadlang.finanzas.categoria.CategoriaRepository;
import com.markmadlang.finanzas.categoria.CategoriaService;
import com.markmadlang.finanzas.movimiento.MovimientoRepository;
import com.markmadlang.finanzas.pagorecurrente.PagoRecurrenteRepository;
import com.markmadlang.finanzas.seguridad.DatosGoogle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final MovimientoRepository movimientoRepository;
    private final PagoRecurrenteRepository pagoRecurrenteRepository;
    private final SaldoBancoRepository saldoBancoRepository;
    private final CategoriaRepository categoriaRepository;
    private final CategoriaService categoriaService;
    private final Clock reloj;

    public UsuarioService(UsuarioRepository repository, MovimientoRepository movimientoRepository,
                          PagoRecurrenteRepository pagoRecurrenteRepository, SaldoBancoRepository saldoBancoRepository,
                          CategoriaRepository categoriaRepository, CategoriaService categoriaService, Clock reloj) {
        this.repository = repository;
        this.movimientoRepository = movimientoRepository;
        this.pagoRecurrenteRepository = pagoRecurrenteRepository;
        this.saldoBancoRepository = saldoBancoRepository;
        this.categoriaRepository = categoriaRepository;
        this.categoriaService = categoriaService;
        this.reloj = reloj;
    }

    /**
     * Alguien ha entrado con Google (y Google ya ha confirmado quién es):
     *  - si ya existía, actualizamos su nombre, email y foto;
     *  - si es la primera vez, le creamos su usuario (ver crear()).
     */
    @Transactional
    public Usuario entrarConGoogle(DatosGoogle datos) {
        return repository.findByGoogleId(datos.googleId())
                .map(usuario -> {
                    usuario.actualizar(datos.email(), datos.nombre(), datos.foto());
                    return usuario;
                })
                .orElseGet(() -> crear(datos));
    }

    /**
     * Un usuario nuevo:
     *  1. Si es el PRIMERO de la app, se queda con todo lo que había antes de que existieran
     *     los usuarios (los datos "sin dueño"). Así no se pierde nada de lo que ya tenías apuntado.
     *  2. Si después de eso no tiene categorías, le damos las de serie (Comida, Vivienda...).
     */
    private Usuario crear(DatosGoogle datos) {
        boolean esElPrimero = repository.count() == 0;
        Usuario nuevo = repository.save(
                new Usuario(datos.googleId(), datos.email(), datos.nombre(), datos.foto(), reloj.instant()));

        if (esElPrimero) {
            categoriaRepository.adoptarLasSinDueno(nuevo.getId());
            movimientoRepository.adoptarLosSinDueno(nuevo.getId());
            pagoRecurrenteRepository.adoptarLosSinDueno(nuevo.getId());
            saldoBancoRepository.adoptarLosSinDueno(nuevo.getId());
        }
        categoriaService.crearLasDeSerieSiNoTiene(nuevo.getId());
        return nuevo;
    }

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
    }
}
