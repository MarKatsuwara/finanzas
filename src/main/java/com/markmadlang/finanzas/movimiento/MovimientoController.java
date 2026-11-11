package com.markmadlang.finanzas.movimiento;

import com.markmadlang.finanzas.seguridad.UsuarioId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Controlador: la "puerta" de la API (el camarero).
 * Recibe las peticiones HTTP, se las pasa al servicio y devuelve la respuesta.
 * @UsuarioId: el id del usuario que hace la petición (sale de su pulsera).
 */
@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {

    private final MovimientoService service;

    public MovimientoController(MovimientoService service) {
        this.service = service;
    }

    @GetMapping
    public List<MovimientoResponse> listar(@UsuarioId Long usuarioId) {
        return service.listar(usuarioId);
    }

    @GetMapping("/{id}")
    public MovimientoResponse obtener(@UsuarioId Long usuarioId, @PathVariable Long id) {
        return service.obtener(usuarioId, id);
    }

    /** @Valid activa las reglas de MovimientoRequest. Responde 201 Created. */
    @PostMapping
    public ResponseEntity<MovimientoResponse> crear(@UsuarioId Long usuarioId, @Valid @RequestBody MovimientoRequest datos) {
        MovimientoResponse creado = service.crear(usuarioId, datos);
        return ResponseEntity.created(URI.create("/api/movimientos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public MovimientoResponse actualizar(@UsuarioId Long usuarioId, @PathVariable Long id,
                                         @Valid @RequestBody MovimientoRequest datos) {
        return service.actualizar(usuarioId, id, datos);
    }

    /** Responde 204 No Content: borrado, y no hay nada que devolver. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@UsuarioId Long usuarioId, @PathVariable Long id) {
        service.borrar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
