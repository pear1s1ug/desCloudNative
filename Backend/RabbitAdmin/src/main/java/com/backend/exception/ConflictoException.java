package com.backend.exception;

/**
 * La operación choca con el estado actual de Kafka: nombre reservado (__*),
 * topic del sistema o recurso que ya existe. Se responde 409.
 */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String message) {
        super(message);
    }
}
