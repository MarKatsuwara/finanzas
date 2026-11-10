package com.markmadlang.finanzas.categoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** El "almacén" de categorías. Spring genera el SQL a partir del nombre de cada método. */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByNombreAsc();

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    /** ¿Ya existe una categoría con ese nombre (sin mirar mayúsculas)? */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Igual, pero sin contar la propia categoría (para cuando la editamos). */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
