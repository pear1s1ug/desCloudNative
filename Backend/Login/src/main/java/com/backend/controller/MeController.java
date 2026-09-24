package com.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Devuelve los claims que el BFF leyó del token ya validado.
 * Sirve para demostrar qué contiene el JWT (roles, scopes, aud, iss, exp).
 */
@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", jwt.getClaimAsString("name"));
        datos.put("usuario", jwt.getClaimAsString("preferred_username"));
        datos.put("oid", jwt.getClaimAsString("oid"));
        datos.put("roles", jwt.getClaimAsStringList("roles"));
        datos.put("scp", jwt.getClaimAsString("scp"));
        datos.put("aud", jwt.getAudience());
        datos.put("iss", jwt.getClaimAsString("iss"));
        datos.put("exp", jwt.getExpiresAt());
        datos.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList());
        return datos;
    }
}