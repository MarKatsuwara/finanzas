package com.markmadlang.finanzas.prevision;

import com.markmadlang.finanzas.seguridad.UsuarioId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** La "puerta" de la API para la previsión del mes. */
@RestController
@RequestMapping("/api/prevision")
public class PrevisionController {

    private final PrevisionService service;

    public PrevisionController(PrevisionService service) {
        this.service = service;
    }

    /** ¿Cómo voy a acabar este mes? */
    @GetMapping
    public PrevisionResponse prevision(@UsuarioId Long usuarioId) {
        return service.prevision(usuarioId);
    }
}
