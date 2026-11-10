package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.categoria.Categoria;
import com.markmadlang.finanzas.movimiento.TipoMovimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Una fila de la tabla "pagos_recurrentes": un pago que se repite cada semana, mes o año. */
@Entity
@Table(name = "pagos_recurrentes")
public class PagoRecurrente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descripcion;

    // Reutilizamos el mismo enum que los movimientos: INGRESO o GASTO
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frecuencia frecuencia;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private boolean activo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    /** JPA necesita un constructor vacío. */
    protected PagoRecurrente() {
    }

    public PagoRecurrente(String descripcion, TipoMovimiento tipo, BigDecimal importe, Frecuencia frecuencia,
                          LocalDate fechaInicio, boolean activo, Categoria categoria) {
        actualizar(descripcion, tipo, importe, frecuencia, fechaInicio, activo, categoria);
    }

    /** Cambia todos los datos de golpe (se usa al crear y al editar). */
    public void actualizar(String descripcion, TipoMovimiento tipo, BigDecimal importe, Frecuencia frecuencia,
                           LocalDate fechaInicio, boolean activo, Categoria categoria) {
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.importe = importe.setScale(2); // siempre con 2 decimales: 650 → 650.00
        this.frecuencia = frecuencia;
        this.fechaInicio = fechaInicio;
        this.activo = activo;
        this.categoria = categoria;
    }

    /**
     * Todas las fechas en las que toca este pago entre "desde" y "hasta" (las dos incluidas).
     * Va probando la repetición 0, 1, 2... hasta pasarse de "hasta".
     */
    public List<LocalDate> fechasEntre(LocalDate desde, LocalDate hasta) {
        List<LocalDate> fechas = new ArrayList<>();
        for (long veces = 0; ; veces++) {
            LocalDate fecha = frecuencia.repeticion(fechaInicio, veces);
            if (fecha.isAfter(hasta)) {
                break; // ya nos hemos pasado del periodo: paramos
            }
            if (!fecha.isBefore(desde)) {
                fechas.add(fecha); // está dentro del periodo: la guardamos
            }
        }
        return fechas;
    }

    /**
     * La primera vez que toca este pago en "desde" o después.
     * Ejemplo: alquiler mensual desde el 01/01 y hoy es 10/11 → 01/12.
     */
    public LocalDate proximaFechaDesde(LocalDate desde) {
        for (long veces = 0; ; veces++) {
            LocalDate fecha = frecuencia.repeticion(fechaInicio, veces);
            if (!fecha.isBefore(desde)) {
                return fecha;
            }
        }
    }

    /**
     * Cuánto cuesta este pago "al mes", repartiendo lo que se paga en un año entre 12.
     * Ejemplos: 420 € al año → 35,00 €/mes; 60 € cada trimestre → 20,00 €/mes; 30 € a la semana → 130,00 €/mes.
     * Redondeo "HALF_UP": el de toda la vida (16,665 → 16,67).
     */
    public BigDecimal importeMensual() {
        return importe.multiply(BigDecimal.valueOf(frecuencia.vecesAlAnio()))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public Frecuencia getFrecuencia() {
        return frecuencia;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public boolean isActivo() {
        return activo;
    }

    public Categoria getCategoria() {
        return categoria;
    }
}
