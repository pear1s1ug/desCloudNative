package com.backend.controller;

import com.backend.dto.CrearTopicRequest;
import com.backend.dto.TopicResponse;
import com.backend.dto.Validaciones;
import com.backend.service.KafkaAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API REST para administrar topics Kafka. Requiere JWT con rol Admin.
 */
@RestController
@RequestMapping("/api/admin/kafka")
@Tag(name = "Administración de Kafka", description = "Topics")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    public KafkaAdminController(KafkaAdminService kafkaAdminService) {
        this.kafkaAdminService = kafkaAdminService;
    }

    @Operation(summary = "Crear un topic")
    @PostMapping("/topics")
    public ResponseEntity<TopicResponse> crearTopic(@Valid @RequestBody CrearTopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kafkaAdminService.crearTopic(request));
    }

    @Operation(summary = "Consultar un topic", description = "Particiones y factor de replicación")
    @GetMapping("/topics/{nombre}")
    public ResponseEntity<TopicResponse> obtenerTopic(
            @PathVariable @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE) String nombre) {
        return ResponseEntity.ok(kafkaAdminService.obtenerTopic(nombre));
    }

    @Operation(summary = "Eliminar un topic", description = "Los topics del sistema no se pueden eliminar (409)")
    @DeleteMapping("/topics/{nombre}")
    public ResponseEntity<Void> eliminarTopic(
            @PathVariable @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE) String nombre) {
        kafkaAdminService.eliminarTopic(nombre);
        return ResponseEntity.noContent().build();
    }
}
