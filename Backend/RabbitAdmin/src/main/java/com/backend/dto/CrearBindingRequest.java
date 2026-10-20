package com.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para enlazar una cola a un exchange con una routing key. */
public record CrearBindingRequest(

        @NotBlank(message = "La cola es obligatoria")
        @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE)
        String cola,

        @NotBlank(message = "El exchange es obligatorio")
        @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE)
        String exchange,

        @NotBlank(message = "La routing key es obligatoria")
        @Size(max = 255, message = "La routing key no puede superar los 255 caracteres")
        @Pattern(regexp = Validaciones.ROUTING_KEY, message = Validaciones.MENSAJE_ROUTING_KEY)
        String routingKey) {
}