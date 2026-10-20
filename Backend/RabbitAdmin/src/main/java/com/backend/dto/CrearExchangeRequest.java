package com.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para crear un exchange. durable: por defecto true. */
public record CrearExchangeRequest(

        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        @Pattern(regexp = Validaciones.NOMBRE, message = Validaciones.MENSAJE_NOMBRE)
        String nombre,

        @NotNull(message = "El tipo es obligatorio: DIRECT, TOPIC o FANOUT")
        TipoExchange tipo,

        Boolean durable) {
}