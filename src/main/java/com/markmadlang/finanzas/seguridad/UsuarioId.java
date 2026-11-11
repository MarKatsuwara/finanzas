package com.markmadlang.finanzas.seguridad;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Una etiqueta para los parámetros de los controladores que significa:
 * "dame el id del usuario que ha hecho esta petición".
 *
 * Spring lo saca de la pulsera (el token JWT): su campo "sub" es el id del usuario.
 * Ejemplo:  public List<...> listar(@UsuarioId Long usuarioId)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal(expression = "T(java.lang.Long).valueOf(subject)")
public @interface UsuarioId {
}
