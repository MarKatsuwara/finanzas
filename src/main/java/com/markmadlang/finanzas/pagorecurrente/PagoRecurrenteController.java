package com.markmadlang.finanzas.pagorecurrente;

import com.markmadlang.finanzas.movimiento.MovimientoResponse;
import com.markmadlang.finanzas.seguridad.UsuarioId;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** La "puerta" de la API para los pagos recurrentes. */
@RestController
@RequestMapping("/api/pagos-recurrentes")
public class PagoRecurrenteController {

    private final PagoRecurrenteService service;
    private final ApuntadorDeFijos apuntador;

    public PagoRecurrenteController(PagoRecurrenteService service, ApuntadorDeFijos apuntador) {
        this.service = service;
        this.apuntador = apuntador;
    }

    @GetMapping
    public List<PagoRecurrenteResponse> listar(@UsuarioId Long usuarioId) {
        return service.listar(usuarioId);
    }

    /**
     * Próximos pagos. Ejemplos:
     *   /api/pagos-recurrentes/proximos                        → desde hoy, 30 días
     *   /api/pagos-recurrentes/proximos?dias=7                 → desde hoy, 7 días
     *   /api/pagos-recurrentes/proximos?desde=2026-12-01&dias=31 → todo diciembre
     */
    @GetMapping("/proximos")
    public List<ProximoPagoResponse> proximos(
            @UsuarioId Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(defaultValue = "30") int dias) {
        return service.proximos(usuarioId, desde != null ? desde : LocalDate.now(), dias);
    }

    /**
     * Apunta como movimientos los fijos que ya han llegado a su día y aún no estaban apuntados.
     * La web lo llama al abrirse. Devuelve lo que ha apuntado (una lista vacía si no había nada).
     */
    @PostMapping("/apuntar-pendientes")
    public List<MovimientoResponse> apuntarPendientes(@UsuarioId Long usuarioId) {
        return apuntador.apuntarPendientes(usuarioId);
    }

    @GetMapping("/{id}")
    public PagoRecurrenteResponse obtener(@UsuarioId Long usuarioId, @PathVariable Long id) {
        return service.obtener(usuarioId, id);
    }

    @PostMapping
    public ResponseEntity<PagoRecurrenteResponse> crear(@UsuarioId Long usuarioId, @Valid @RequestBody PagoRecurrenteRequest datos) {
        PagoRecurrenteResponse creado = service.crear(usuarioId, datos);
        return ResponseEntity.created(URI.create("/api/pagos-recurrentes/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public PagoRecurrenteResponse actualizar(@UsuarioId Long usuarioId, @PathVariable Long id,
                                             @Valid @RequestBody PagoRecurrenteRequest datos) {
        return service.actualizar(usuarioId, id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@UsuarioId Long usuarioId, @PathVariable Long id) {
        service.borrar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}
