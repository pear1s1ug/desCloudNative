package com.backend.messaging.event;

import java.time.LocalDateTime;

public record TicketCreadoEvent(Long ticketId, String usuarioOid, String asunto, String mensaje, LocalDateTime fecha) {}