package com.backend.messaging.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockBajoEvent(Long productoId, String nombre, BigDecimal precio, Integer stock, LocalDateTime fecha) {}