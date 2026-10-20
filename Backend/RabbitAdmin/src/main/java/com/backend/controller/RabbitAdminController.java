package com.backend.controller;

import com.backend.dto.ColaResponse;
import com.backend.dto.CrearBindingRequest;
import com.backend.dto.CrearColaRequest;
import com.backend.dto.CrearExchangeRequest;
import com.backend.dto.Validaciones;
import com.backend.service.RabbitAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ==========================================
 * CAPA DE CONTROLADOR (CONTROLLER - MVC)
 * ==========================================
 * API REST para administrar RabbitMQ. Requiere JWT con rol Admin.
 * Solo valida la entrada y delega en RabbitAdminService: no conoce detalles de RabbitMQ.
 */
@RestController
@RequestMapping("/api/admin/rabbit")
@Tag(name = "Administración de RabbitMQ", description = "Colas, exchanges y bindings")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    // ===================== COLAS =====================

    @Operation(summary = "Crear una cola", description = "Opcionalmente con DLQ (<nombre>.dlq) y TTL en milisegundos")
    @PostMapping("/queues")
    public ResponseEntity<ColaResponse> crearCola(@Valid @RequestBody CrearColaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.crearCola(request));
    }

    @Operation(summary = "Consultar una cola", description = "Mensajes en espera y consumidores conectados")
    @GetMapping("/queues/{nombre}")
    public ResponseEntity<ColaResponse> obtenerCola(
            @PathVariable @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE) String nombre) {
        return ResponseEntity.ok(rabbitAdminService.obtenerCola(nombre));
    }

    @Operation(summary = "Eliminar una cola", description = "Las colas del sistema no se pueden eliminar (409)")
    @DeleteMapping("/queues/{nombre}")
    public ResponseEntity<Void> eliminarCola(
            @PathVariable @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE) String nombre) {
        rabbitAdminService.eliminarCola(nombre);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vaciar una cola", description = "Elimina los mensajes en espera")
    @PostMapping("/queues/{nombre}/purge")
    public ResponseEntity<Map<String, Object>> purgarCola(
            @PathVariable @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE) String nombre) {
        int eliminados = rabbitAdminService.purgarCola(nombre);
        return ResponseEntity.ok(Map.of("cola", nombre, "eliminados", eliminados));
    }

    // ===================== EXCHANGES =====================

    @Operation(summary = "Crear un exchange", description = "Tipos permitidos: DIRECT, TOPIC, FANOUT")
    @PostMapping("/exchanges")
    public ResponseEntity<CrearExchangeRequest> crearExchange(@Valid @RequestBody CrearExchangeRequest request) {
        rabbitAdminService.crearExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @Operation(summary = "Eliminar un exchange", description = "Los exchanges del sistema no se pueden eliminar (409)")
    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Void> eliminarExchange(
            @PathVariable @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE) String nombre) {
        rabbitAdminService.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }

    // ===================== BINDINGS =====================

    @Operation(summary = "Enlazar una cola a un exchange", description = "Con routing key; admite comodines * y # para topic")
    @PostMapping("/bindings")
    public ResponseEntity<CrearBindingRequest> crearBinding(@Valid @RequestBody CrearBindingRequest request) {
        rabbitAdminService.crearBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @Operation(summary = "Eliminar un binding", description = "Se identifica por cola, exchange y routing key")
    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(
            @RequestParam @NotBlank @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE) String cola,
            @RequestParam @NotBlank @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE) String exchange,
            @RequestParam @NotBlank @Pattern(regexp = Validaciones.ROUTING_KEY, message = Validaciones.MENSAJE_ROUTING_KEY) String routingKey) {
        rabbitAdminService.eliminarBinding(cola, exchange, routingKey);
        return ResponseEntity.noContent().build();
    }
}