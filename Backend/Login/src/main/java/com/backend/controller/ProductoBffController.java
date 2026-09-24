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
 * Rutas de productos expuestas por el BFF.
 * Reenvía cada petición al microservicio de productos con el token del usuario.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoBffController {

    private static final String BASE = "/api/productos";

    private final RestClient productos;
    private final DownstreamService downstream;

    public ProductoBffController(@Qualifier("productosClient") RestClient productos, DownstreamService downstream) {
        this.productos = productos;
        this.downstream = downstream;
    }

    @GetMapping
    public ResponseEntity<String> listar(@AuthenticationPrincipal Jwt jwt) {
        return downstream.forward(productos, HttpMethod.GET, BASE, null, jwt);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return downstream.forward(productos, HttpMethod.GET, BASE + "/" + id, null, jwt);
    }

    @PostMapping
    public ResponseEntity<String> crear(@RequestBody String body, @AuthenticationPrincipal Jwt jwt) {
        return downstream.forward(productos, HttpMethod.POST, BASE, body, jwt);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> actualizar(@PathVariable Long id, @RequestBody String body,
                                             @AuthenticationPrincipal Jwt jwt) {
        return downstream.forward(productos, HttpMethod.PUT, BASE + "/" + id, body, jwt);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return downstream.forward(productos, HttpMethod.DELETE, BASE + "/" + id, null, jwt);
    }
}