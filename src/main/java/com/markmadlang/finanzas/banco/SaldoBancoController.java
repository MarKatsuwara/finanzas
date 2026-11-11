package com.markmadlang.finanzas.banco;

import com.markmadlang.finanzas.seguridad.UsuarioId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** La "puerta" de la API para el saldo del banco. */
@RestController
@RequestMapping("/api/saldo-banco")
public class SaldoBancoController {

    private final SaldoBancoService service;

    public SaldoBancoController(SaldoBancoService service) {
        this.service = service;
    }

    /** Apuntar lo que tienes en el banco: {"fecha": "2026-11-10", "importe": 400} */
    @PostMapping
    public ResponseEntity<SaldoApuntadoResponse> apuntar(@UsuarioId Long usuarioId, @Valid @RequestBody SaldoBancoRequest datos) {
        SaldoApuntadoResponse apuntado = service.apuntar(usuarioId, datos);
        return ResponseEntity.created(URI.create("/api/saldo-banco")).body(apuntado);
    }

    /** ¿Cuánto tengo hoy? */
    @GetMapping
    public SaldoActualResponse saldoActual(@UsuarioId Long usuarioId) {
        return service.saldoActual(usuarioId);
    }

    /** Todos los saldos que has apuntado, del más reciente al más antiguo. */
    @GetMapping("/historial")
    public List<SaldoApuntadoResponse> historial(@UsuarioId Long usuarioId) {
        return service.historial(usuarioId);
    }

    /** ¿Cómo irá mi saldo? Ejemplo: /api/saldo-banco/prevision?dias=60 (por defecto 30) */
    @GetMapping("/prevision")
    public PrevisionBancoResponse prevision(@UsuarioId Long usuarioId, @RequestParam(defaultValue = "30") int dias) {
        return service.prevision(usuarioId, dias);
    }
}
