package com.backend.controller;

import com.backend.service.DownstreamService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

/**
 * Rutas de usuarios expuestas por el BFF.
 * Reenvía cada petición al microservicio de usuarios con el token del usuario.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioBffController {

    private static final String BASE = "/api/usuarios";

    private final RestClient usuarios;
    private final DownstreamService downstream;

    public UsuarioBffController(@Qualifier("usuariosClient") RestClient usuarios, DownstreamService downstream) {
        this.usuarios = usuarios;
        this.downstream = downstream;
    }

    /** Perfil del usuario autenticado (se crea o actualiza desde los claims del token). */
    @GetMapping("/me")
    public ResponseEntity<String> miPerfil(@AuthenticationPrincipal Jwt jwt) {
        return downstream.forward(usuarios, HttpMethod.GET, BASE + "/me", null, jwt);
    }

    @GetMapping
    public ResponseEntity<String> listar(@AuthenticationPrincipal Jwt jwt) {
        return downstream.forward(usuarios, HttpMethod.GET, BASE, null, jwt);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return downstream.forward(usuarios, HttpMethod.GET, BASE + "/" + id, null, jwt);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> actualizar(@PathVariable Long id, @RequestBody String body,
                                             @AuthenticationPrincipal Jwt jwt) {
        return downstream.forward(usuarios, HttpMethod.PUT, BASE + "/" + id, body, jwt);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return downstream.forward(usuarios, HttpMethod.DELETE, BASE + "/" + id, null, jwt);
    }
}