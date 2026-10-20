package com.backend.messaging.event;

import java.time.Instant;
import java.util.List;

/** Evento de compra para generar el ticket. Contrato en infra/eventos.md. */
public record OrdenCreadaEvent(
        Long ordenId,
        String usuarioOid,
        List<Item> items,
        Double total,
        Instant fecha) {

    public record Item(
            Long productoId,
            String nombre,
            Integer cantidad,
            Double precioUnitario) {}
}
