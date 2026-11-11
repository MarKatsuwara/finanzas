package com.markmadlang.finanzas.seguridad;

/**
 * Comprueba el "carné" (la credencial) que Google da a la web cuando alguien pulsa
 * "Iniciar sesión con Google", y dice de quién es.
 *
 * Es una interfaz para que en los tests podamos usar uno de mentira
 * (los tests no pueden iniciar sesión en Google de verdad).
 */
public interface VerificadorGoogle {

    /**
     * @throws CredencialNoValidaException si el carné es falso, está caducado o es de otra app
     */
    DatosGoogle verificar(String credencial);
}
