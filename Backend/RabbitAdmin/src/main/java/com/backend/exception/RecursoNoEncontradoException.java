package com.backend.exception;

/** El topic pedido no existe en Kafka. Se responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
