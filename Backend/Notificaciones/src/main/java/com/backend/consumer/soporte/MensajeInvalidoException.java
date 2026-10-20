package com.backend.consumer.soporte;

public class MensajeInvalidoException extends RuntimeException {
    public MensajeInvalidoException(String message) {
        super(message);
    }
}