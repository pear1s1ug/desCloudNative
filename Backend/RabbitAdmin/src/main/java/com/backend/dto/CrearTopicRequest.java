package com.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CrearTopicRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE)
        String nombre,

        @Min(value = 1, message = "partitions debe ser >= 1")
        @Max(value = 100, message = "partitions debe ser <= 100")
        Integer partitions,

        @Min(value = 1, message = "replicationFactor debe ser >= 1")
        @Max(value = 10, message = "replicationFactor debe ser <= 10")
        Short replicationFactor) {
}
