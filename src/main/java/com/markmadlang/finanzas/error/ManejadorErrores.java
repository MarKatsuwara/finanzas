package com.markmadlang.finanzas.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte los errores en respuestas claras para quien usa la API,
 * en lugar de una página de error genérica.
 */
@RestControllerAdvice
public class ManejadorErrores {

    /** Algo que no existe → 404 Not Found */
    @ExceptionHandler(NoEncontradoException.class)
    public ProblemDetail noEncontrado(NoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Choca con lo que ya hay guardado → 409 Conflict */
    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail conflicto(ConflictoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Datos que no cumplen las reglas (@NotNull, @Positive...) → 400 Bad Request */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail datosNoValidos(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return problemaDeDatos(errores);
    }

    /** Un campo con un valor imposible (categoría que no existe...) → 400 Bad Request */
    @ExceptionHandler(CampoNoValidoException.class)
    public ProblemDetail campoNoValido(CampoNoValidoException e) {
        return problemaDeDatos(Map.of(e.getCampo(), e.getMessage()));
    }

    /** JSON mal escrito o con un valor que no existe ("DIARIA", una fecha imposible...) → 400 Bad Request */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonNoValido(HttpMessageNotReadableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "No se ha podido leer la petición: revisa que el JSON esté bien escrito y que los valores existan "
                        + "(tipo: INGRESO o GASTO; frecuencia: SEMANAL, CADA_4_SEMANAS, MENSUAL, BIMESTRAL, TRIMESTRAL, SEMESTRAL o ANUAL; "
                        + "fechas: AAAA-MM-DD)");
    }

    /** Un dato de la URL con formato incorrecto (?dias=muchos, ?desde=ayer...) → 400 Bad Request */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail parametroNoValido(MethodArgumentTypeMismatchException e) {
        return problemaDeDatos(Map.of(e.getName(), "El valor '" + e.getValue() + "' no es válido"));
    }

    private ProblemDetail problemaDeDatos(Map<String, String> errores) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Hay datos que no son válidos");
        problema.setProperty("errores", errores);
        return problema;
    }
}
