package com.backend.consumer.soporte;

/**
 * Error NO recuperable: el mensaje trae datos inválidos y reintentarlo no sirve.
 * DefaultErrorHandler lo envía directo al topic DLT (excepción no reintentable).
 */
public class MensajeInvalidoException extends RuntimeException {
    public MensajeInvalidoException(String message) {
        super(message);
    }
}
