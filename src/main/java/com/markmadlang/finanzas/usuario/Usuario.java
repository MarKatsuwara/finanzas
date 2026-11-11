package com.markmadlang.finanzas.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Una fila de la tabla "usuarios": una persona que entra con su cuenta de Google. */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", nullable = false, unique = true)
    private String googleId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 1000)
    private String foto;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    /** JPA necesita un constructor vacío. */
    protected Usuario() {
    }

    public Usuario(String googleId, String email, String nombre, String foto, Instant creadoEn) {
        this.googleId = googleId;
        this.email = email;
        this.nombre = nombre;
        this.foto = foto;
        this.creadoEn = creadoEn;
    }

    /** Cada vez que entra, copiamos lo que diga Google (por si ha cambiado de nombre o de foto). */
    public void actualizar(String email, String nombre, String foto) {
        this.email = email;
        this.nombre = nombre;
        this.foto = foto;
    }

    public Long getId() {
        return id;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getFoto() {
        return foto;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
