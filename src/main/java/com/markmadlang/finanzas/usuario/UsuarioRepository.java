package com.markmadlang.finanzas.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** El "almacén" de usuarios. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByGoogleId(String googleId);
}
