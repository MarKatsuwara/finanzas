package com.markmadlang.finanzas.usuario;

import com.markmadlang.finanzas.seguridad.DatosGoogle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final Clock reloj;

    public UsuarioService(UsuarioRepository repository, Clock reloj) {
        this.repository = repository;
        this.reloj = reloj;
    }

    /**
     * Alguien ha entrado con Google (y Google ya ha confirmado quién es):
     *  - si es la primera vez, le creamos su usuario;
     *  - si ya existía, actualizamos su nombre, email y foto.
     */
    @Transactional
    public Usuario entrarConGoogle(DatosGoogle datos) {
        return repository.findByGoogleId(datos.googleId())
                .map(usuario -> {
                    usuario.actualizar(datos.email(), datos.nombre(), datos.foto());
                    return usuario;
                })
                .orElseGet(() -> repository.save(
                        new Usuario(datos.googleId(), datos.email(), datos.nombre(), datos.foto(), reloj.instant())));
    }

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
    }
}
