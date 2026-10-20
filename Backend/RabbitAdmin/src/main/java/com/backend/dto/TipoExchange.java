package com.backend.dto;

/** Tipos de exchange que la API permite crear. Cualquier otro valor responde 400. */
public enum TipoExchange {
    DIRECT,
    TOPIC,
    FANOUT
}