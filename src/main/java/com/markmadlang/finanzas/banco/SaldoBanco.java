package com.markmadlang.finanzas.banco;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Una fila de "saldos_banco": cuánto dinero tenías en el banco en una fecha. */
@Entity
@Table(name = "saldos_banco")
public class SaldoBanco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // De quién es (el id del usuario). Solo puede ser null en los datos de antes de que existieran usuarios.
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    /** JPA necesita un constructor vacío. */
    protected SaldoBanco() {
    }

    public SaldoBanco(Long usuarioId, LocalDate fecha, BigDecimal importe) {
        this.usuarioId = usuarioId;
        this.fecha = fecha;
        this.importe = importe.setScale(2);
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public BigDecimal getImporte() {
        return importe;
    }
}
