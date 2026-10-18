package com.backend.messaging.event;

import java.time.LocalDateTime;

public record UsuarioRegistradoEvent(String azureOid, String nombre, String email, LocalDateTime fecha) {}