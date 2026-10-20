package com.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una cola.
 * durable: por defecto true. conDlq: crea también <nombre>.dlq enlazada al DLX. ttlMs: opcional.
 */
public record CrearColaRequest(

        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE)
        String nombre,

        Boolean durable,

        Boolean conDlq,

        @PositiveOrZero(message = "El TTL no puede ser negativo")
        @Max(value = 86400000, message = "El TTL no puede superar 24 horas (86400000 ms)")
        Long ttlMs) {
}