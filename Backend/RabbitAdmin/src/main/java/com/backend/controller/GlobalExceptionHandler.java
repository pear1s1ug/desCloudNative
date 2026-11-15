package com.backend.controller;

import com.backend.exception.ConflictoException;
import com.backend.exception.RecursoNoEncontradoException;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce los errores a respuestas JSON claras, con el mismo formato que los otros servicios:
 * 400 entrada inválida · 404 recurso inexistente · 409 conflicto · 503 Kafka no disponible.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> camposInvalidos(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", errores);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> parametrosInvalidos(HandlerMethodValidationException ex) {
        List<String> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetros inválidos", errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "Cuerpo inválido: revisa el formato JSON y los valores permitidos", null);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(KafkaException.class)
    public ResponseEntity<Map<String, Object>> errorKafka(KafkaException ex) {
        String detalle = String.valueOf(ex.getMessage());
        if (detalle.toLowerCase().contains("timeout") || detalle.toLowerCase().contains("disconnect")
                || detalle.toLowerCase().contains("connection")) {
            log.error("Kafka no disponible: {}", detalle);
            return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no está disponible, intenta más tarde", null);
        }
        log.error("Error de Kafka: {}", detalle);
        return respuesta(HttpStatus.BAD_GATEWAY, "Kafka rechazó la operación", null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> estadoInvalido(IllegalStateException ex) {
        String detalle = String.valueOf(ex.getMessage());
        if (detalle.toLowerCase().contains("timeout") || detalle.toLowerCase().contains("kafka")) {
            log.error("Kafka no disponible: {}", detalle);
            return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no está disponible, intenta más tarde", null);
        }
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, detalle, null);
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String mensaje, List<String> errores) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        if (errores != null && !errores.isEmpty()) {
            body.put("errores", errores);
        }
        return ResponseEntity.status(status).body(body);
    }
}
