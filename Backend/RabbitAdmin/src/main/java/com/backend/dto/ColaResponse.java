package com.backend.dto;

/** Estado de una cola: mensajes en espera y consumidores conectados. */
public record ColaResponse(String nombre, long mensajes, int consumidores) {}