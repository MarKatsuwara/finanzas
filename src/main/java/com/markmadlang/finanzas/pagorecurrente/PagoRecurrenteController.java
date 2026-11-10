package com.markmadlang.finanzas.pagorecurrente;

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

/** La "puerta" de la API para los pagos recurrentes. */
@RestController
@RequestMapping("/api/pagos-recurrentes")
public class PagoRecurrenteController {

    private final PagoRecurrenteService service;

    public PagoRecurrenteController(PagoRecurrenteService service) {
        this.service = service;
    }

    @GetMapping
    public List<PagoRecurrenteResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public PagoRecurrenteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<PagoRecurrenteResponse> crear(@Valid @RequestBody PagoRecurrenteRequest datos) {
        PagoRecurrenteResponse creado = service.crear(datos);
        return ResponseEntity.created(URI.create("/api/pagos-recurrentes/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public PagoRecurrenteResponse actualizar(@PathVariable Long id, @Valid @RequestBody PagoRecurrenteRequest datos) {
        return service.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Long id) {
        service.borrar(id);
        return ResponseEntity.noContent().build();
    }
}
