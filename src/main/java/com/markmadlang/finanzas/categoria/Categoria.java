package com.markmadlang.finanzas.categoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Una fila de la tabla "categorias" (Comida, Ocio, Vivienda...). */
@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // De quién es (el id del usuario). Solo puede ser null en los datos de antes de que existieran usuarios.
    @Column(name = "usuario_id")
    private Long usuarioId;

    // Único para cada usuario (sin mirar mayúsculas): lo comprueba CategoriaService
    @Column(nullable = false, length = 50)
    private String nombre;

    /** JPA necesita un constructor vacío. */
    protected Categoria() {
    }

    public Categoria(Long usuarioId, String nombre) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
    }

    public void cambiarNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getNombre() {
        return nombre;
    }
}
