package com.markmadlang.finanzas;

import com.markmadlang.finanzas.error.ConflictoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Cuántas cosas puede guardar, como mucho, cada usuario.
 *
 * ¿Por qué? Cualquiera con una cuenta de Google puede entrar, y la base de datos gratis tiene 0,5 GB.
 * Sin límite, alguien con mala idea podría hacer un programa que apuntara millones de movimientos,
 * llenarla y dejar la app sin funcionar PARA TODOS. Los límites son tan generosos que una persona
 * normal no llega nunca (20.000 movimientos son más de 10 al día durante 5 años).
 *
 * Los números salen de application.yaml (finanzas.limites.*), así los tests pueden poner límites pequeños
 * sin tener que crear 20.000 movimientos.
 */
@Component
public class Limites {

    private final int movimientos;
    private final int categorias;
    private final int gastosFijos;
    private final int saldos;

    public Limites(@Value("${finanzas.limites.movimientos:20000}") int movimientos,
                   @Value("${finanzas.limites.categorias:100}") int categorias,
                   @Value("${finanzas.limites.gastos-fijos:200}") int gastosFijos,
                   @Value("${finanzas.limites.saldos:5000}") int saldos) {
        this.movimientos = movimientos;
        this.categorias = categorias;
        this.gastosFijos = gastosFijos;
        this.saldos = saldos;
    }

    /** Antes de crear un movimiento: ¿le cabe uno más? Si no, 409 con un mensaje claro. */
    public void comprobarMovimientos(long losQueTiene) {
        comprobar(losQueTiene, movimientos, "movimientos");
    }

    public void comprobarCategorias(long lasQueTiene) {
        comprobar(lasQueTiene, categorias, "categorías");
    }

    public void comprobarGastosFijos(long losQueTiene) {
        comprobar(losQueTiene, gastosFijos, "gastos fijos");
    }

    public void comprobarSaldos(long losQueTiene) {
        comprobar(losQueTiene, saldos, "saldos del banco apuntados");
    }

    private void comprobar(long tiene, int maximo, String cosas) {
        if (tiene >= maximo) {
            String numero = NumberFormat.getIntegerInstance(Locale.of("es", "ES")).format(maximo);
            throw new ConflictoException("Has llegado al máximo de " + numero + " " + cosas
                    + ". Borra alguno para poder guardar más.");
        }
    }
}
