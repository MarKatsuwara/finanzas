package com.markmadlang.finanzas.movimiento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad: representa UNA fila de la tabla "movimientos".
 * Hibernate (JPA) se encarga de convertir objetos Java en filas y al revés.
 */
@Entity
@Table(name = "movimientos")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // el id lo pone PostgreSQL
    private Long id;

    @Enumerated(EnumType.STRING) // se guarda como texto: "INGRESO" o "GASTO"
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    // Dinero SIEMPRE con BigDecimal: double redondea mal (0.1 + 0.2 = 0.30000000000000004)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private String descripcion;

    /** JPA necesita un constructor vacío. Es protected para que no lo usemos nosotros. */
    protected Movimiento() {
    }

    public Movimiento(TipoMovimiento tipo, BigDecimal importe, LocalDate fecha, String descripcion) {
        actualizar(tipo, importe, fecha, descripcion);
    }

    /** Cambia todos los datos de golpe (se usa al crear y al editar). */
    public void actualizar(TipoMovimiento tipo, BigDecimal importe, LocalDate fecha, String descripcion) {
        this.tipo = tipo;
        this.importe = importe;
        this.fecha = fecha;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
