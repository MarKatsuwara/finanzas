package com.markmadlang.finanzas;

import com.markmadlang.finanzas.movimiento.MovimientoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte los errores en respuestas claras para quien usa la API,
 * en lugar de una página de error genérica.
 */
@RestControllerAdvice
public class ManejadorErrores {

    /** Id que no existe → 404 Not Found */
    @ExceptionHandler(MovimientoNoEncontradoException.class)
    public ProblemDetail noEncontrado(MovimientoNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Datos que no cumplen las reglas → 400 Bad Request, con el motivo de cada campo */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail datosNoValidos(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Hay datos que no son válidos");
        problema.setProperty("errores", errores);
        return problema;
    }
}
