package com.backend.consumer.soporte;

/**
 * Error NO recuperable: el mensaje trae datos inválidos y reintentarlo no sirve.
 * ProcesadorConAck lo envía directo a la DLQ (NACK sin requeue).
 */
public class MensajeInvalidoException extends RuntimeException {
    public MensajeInvalidoException(String message) {
        super(message);
    }
}
