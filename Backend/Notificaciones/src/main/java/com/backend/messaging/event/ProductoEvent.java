package com.backend.messaging.event;

import java.time.Instant;

/** Evento de inventario. Contrato en infra/eventos.md. */
public record ProductoEvent(
        Long productoId,
        String nombre,
        Integer stock,
        String accion,   // CREADO | ACTUALIZADO | ELIMINADO | STOCK_BAJO
        Instant fecha) {}
