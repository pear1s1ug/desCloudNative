package com.backend.messaging.event;

import java.time.Instant;

public record UsuarioRegistradoEvent(
        String azureOid,
        String nombre,
        String email,
        Instant fecha) {}