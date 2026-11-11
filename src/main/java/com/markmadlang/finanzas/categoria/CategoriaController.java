package com.markmadlang.finanzas.categoria;

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

/** La "puerta" de la API para las categorías. Funciona igual que la de movimientos. */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoriaResponse> listar(@UsuarioId Long usuarioId) {
        return service.listar(usuarioId);
    }

    @GetMapping("/{id}")
    public CategoriaResponse obtener(@UsuarioId Long usuarioId, @PathVariable Long id) {
        return service.obtener(usuarioId, id);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@UsuarioId Long usuarioId, @Valid @RequestBody CategoriaRequest datos) {
        CategoriaResponse creada = service.crear(usuarioId, datos);
        return ResponseEntity.created(URI.create("/api/categorias/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public CategoriaResponse actualizar(@UsuarioId Long usuarioId, @PathVariable Long id,
                                        @Valid @RequestBody CategoriaRequest datos) {
        return service.actualizar(usuarioId, id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@UsuarioId Long usuarioId, @PathVariable Long id) {
        service.borrar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
