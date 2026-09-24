package com.backend.service;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Reenvía la petición al microservicio con el mismo token (Bearer)
 * que ya validó el BFF, y devuelve su respuesta tal cual.
 */
@Service
public class DownstreamService {

    public ResponseEntity<String> forward(RestClient client, HttpMethod method, String path, String body, Jwt jwt) {
        try {
            RestClient.RequestBodySpec request = client.method(method)
                    .uri(path)
                    .headers(h -> h.setBearerAuth(jwt.getTokenValue()));

            if (body != null) {
                request = request.contentType(MediaType.APPLICATION_JSON).body(body);
            }

            ResponseEntity<String> response = request.retrieve().toEntity(String.class);
            return build(response.getStatusCode().value(), response.getBody());

        } catch (RestClientResponseException e) {
            // 4xx/5xx del microservicio: se devuelve el mismo código y cuerpo
            return build(e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            // El microservicio no responde
            return build(503, "{\"status\":503,\"error\":\"Service Unavailable\",\"mensaje\":\"Microservicio no disponible\"}");
        }
    }

    private ResponseEntity<String> build(int status, String body) {
        if (body == null || body.isBlank()) {
            return ResponseEntity.status(status).build();
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}