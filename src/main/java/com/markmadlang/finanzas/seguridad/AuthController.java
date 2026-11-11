package com.markmadlang.finanzas.seguridad;

import com.markmadlang.finanzas.usuario.Usuario;
import com.markmadlang.finanzas.usuario.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Entrar en la app:
 *  POST /api/auth/google → cambia el carné de Google por una pulsera de nuestra app
 *  GET  /api/auth/yo     → quién soy (según la pulsera que traigo)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final VerificadorGoogle verificadorGoogle;
    private final UsuarioService usuarioService;
    private final ServicioTokens servicioTokens;

    public AuthController(VerificadorGoogle verificadorGoogle, UsuarioService usuarioService, ServicioTokens servicioTokens) {
        this.verificadorGoogle = verificadorGoogle;
        this.usuarioService = usuarioService;
        this.servicioTokens = servicioTokens;
    }

    @PostMapping("/google")
    public SesionResponse entrarConGoogle(@Valid @RequestBody LoginGoogleRequest peticion) {
        DatosGoogle datos = verificadorGoogle.verificar(peticion.credencial()); // 1. ¿El carné es de verdad?
        Usuario usuario = usuarioService.entrarConGoogle(datos);                // 2. Crear o actualizar el usuario
        ServicioTokens.Token token = servicioTokens.crear(usuario);             // 3. Darle su pulsera
        return new SesionResponse(token.valor(), token.caducaEn(), UsuarioResponse.desde(usuario));
    }

    /** @UsuarioId: Spring saca el id del usuario de la pulsera (ya comprobada). */
    @GetMapping("/yo")
    public UsuarioResponse yo(@UsuarioId Long usuarioId) {
        return UsuarioResponse.desde(usuarioService.buscar(usuarioId));
    }

    /** Lo que manda la web: el carné que le ha dado Google. */
    public record LoginGoogleRequest(@NotBlank(message = "Falta la credencial de Google") String credencial) {
    }

    /** Lo que devolvemos al entrar: la pulsera, hasta cuándo vale y quién eres. */
    public record SesionResponse(String token, Instant caducaEn, UsuarioResponse usuario) {
    }

    public record UsuarioResponse(Long id, String email, String nombre, String foto) {
        static UsuarioResponse desde(Usuario usuario) {
            return new UsuarioResponse(usuario.getId(), usuario.getEmail(), usuario.getNombre(), usuario.getFoto());
        }
    }
}
