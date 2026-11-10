package com.markmadlang.finanzas.banco;

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
    public ResponseEntity<SaldoApuntadoResponse> apuntar(@Valid @RequestBody SaldoBancoRequest datos) {
        SaldoApuntadoResponse apuntado = service.apuntar(datos);
        return ResponseEntity.created(URI.create("/api/saldo-banco")).body(apuntado);
    }

    /** ¿Cuánto tengo hoy? */
    @GetMapping
    public SaldoActualResponse saldoActual() {
        return service.saldoActual();
    }

    /** Todos los saldos que has apuntado, del más reciente al más antiguo. */
    @GetMapping("/historial")
    public List<SaldoApuntadoResponse> historial() {
        return service.historial();
    }

    /** ¿Cómo irá mi saldo? Ejemplo: /api/saldo-banco/prevision?dias=60 (por defecto 30) */
    @GetMapping("/prevision")
    public PrevisionBancoResponse prevision(@RequestParam(defaultValue = "30") int dias) {
        return service.prevision(dias);
    }
}
