package com.backend.messaging.event;

import java.time.Instant;

/** Evento de usuario nuevo para la notificación de bienvenida. Contrato en infra/eventos.md. */
public record UsuarioRegistradoEvent(
        String azureOid,
        String nombre,
        String email,
        Instant fecha) {}
