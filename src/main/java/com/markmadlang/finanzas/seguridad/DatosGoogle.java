package com.markmadlang.finanzas.seguridad;

/**
 * Lo que sabemos de una persona después de que Google confirme quién es.
 *
 * @param googleId el identificador de la cuenta en Google (nunca cambia, aunque cambie el email)
 * @param foto     la dirección de su foto (puede ser null)
 */
public record DatosGoogle(String googleId, String email, String nombre, String foto) {
}
