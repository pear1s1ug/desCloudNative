package com.backend.exception;

/** La cola, exchange o binding pedido no existe en RabbitMQ. Se responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
