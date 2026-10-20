package com.backend.exception;

/**
 * La operación choca con el estado actual de RabbitMQ: nombre reservado (amq.*),
 * recurso del sistema protegido o recurso existente con otra configuración. Se responde 409.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
