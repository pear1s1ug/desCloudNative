package com.backend.controller;

import com.backend.exception.ConflictoException;
import com.backend.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
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
 * 400 entrada inválida · 404 recurso inexistente · 409 conflicto · 503 RabbitMQ no disponible.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 400: campos inválidos en el cuerpo (@Valid sobre los DTO). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> camposInvalidos(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", errores);
    }

    /** 400: parámetros de ruta o de consulta inválidos (@Pattern en {nombre}, ?cola=, etc.). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> parametrosInvalidos(HandlerMethodValidationException ex) {
        List<String> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetros inválidos", errores);
    }

    /** 400: JSON mal formado o valor no permitido (por ejemplo, un tipo de exchange inexistente). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "Cuerpo inválido: revisa el formato JSON y los valores permitidos (tipo: DIRECT, TOPIC o FANOUT)", null);
    }

    /** 404: la cola o el exchange no existe. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    /** 409: nombre reservado, recurso del sistema o recurso que ya existe. */
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    /** 503: no hay conexión con RabbitMQ. */
    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<Map<String, Object>> brokerNoDisponible(AmqpConnectException ex) {
        log.error("RabbitMQ no disponible: {}", ex.getMessage());
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ no está disponible, intenta más tarde", null);
    }

    /** 409 si RabbitMQ rechaza la declaración por argumentos distintos (PRECONDITION_FAILED); 502 en otro caso. */
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, Object>> errorRabbit(AmqpException ex) {
        String detalle = String.valueOf(ex.getMessage()) + " " + String.valueOf(ex.getCause());
        if (detalle.contains("PRECONDITION_FAILED")) {
            return respuesta(HttpStatus.CONFLICT, "El recurso ya existe en RabbitMQ con otra configuración", null);
        }
        log.error("Error de RabbitMQ: {}", detalle);
        return respuesta(HttpStatus.BAD_GATEWAY, "RabbitMQ rechazó la operación", null);
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