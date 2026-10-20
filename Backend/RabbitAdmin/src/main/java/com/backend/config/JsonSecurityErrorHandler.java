package com.backend.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401: token ausente, expirado, firma inválida, issuer o audience incorrectos.
 * 403: token válido pero sin el scope o rol requerido.
 */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
    private final BearerTokenAccessDeniedHandler deniedHandler = new BearerTokenAccessDeniedHandler();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException, ServletException {
        entryPoint.commence(request, response, ex);
        write(response, 401, "Unauthorized", "Token ausente, expirado o inválido");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException, ServletException {
        deniedHandler.handle(request, response, ex);
        write(response, 403, "Forbidden", "El token no tiene el rol o scope requerido");
    }

    private void write(HttpServletResponse response, int status, String error, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"status\":%d,\"error\":\"%s\",\"mensaje\":\"%s\"}", status, error, mensaje));
    }
}