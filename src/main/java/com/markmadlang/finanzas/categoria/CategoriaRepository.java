package com.markmadlang.finanzas.categoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** El "almacén" de categorías. Spring genera el SQL a partir del nombre de cada método. */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByUsuarioIdOrderByNombreAsc(Long usuarioId);

    /** Una categoría, pero solo si es de ese usuario. */
    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<Categoria> findByUsuarioIdAndNombreIgnoreCase(Long usuarioId, String nombre);

    /** ¿Este usuario ya tiene una categoría con ese nombre (sin mirar mayúsculas)? */
    boolean existsByUsuarioIdAndNombreIgnoreCase(Long usuarioId, String nombre);

    /** Igual, pero sin contar la propia categoría (para cuando la editamos). */
    boolean existsByUsuarioIdAndNombreIgnoreCaseAndIdNot(Long usuarioId, String nombre, Long id);

    long countByUsuarioId(Long usuarioId);

    /** Las categorías "sin dueño" (las de serie de antes de que hubiera usuarios) pasan a ser de este usuario. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Categoria c SET c.usuarioId = :usuarioId WHERE c.usuarioId IS NULL")
    int adoptarLasSinDueno(@Param("usuarioId") Long usuarioId);
}
