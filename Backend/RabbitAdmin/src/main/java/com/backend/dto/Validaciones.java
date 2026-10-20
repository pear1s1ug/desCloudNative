package com.backend.dto;

/** Reglas de formato compartidas por los DTOs de administración. */
public final class Validaciones {

    /** Minúsculas y números, separados por un punto, guion o guion bajo. Ej: nexus.pagos.queue */
    public static final String NOMBRE = "^[a-z0-9]+([._-][a-z0-9]+)*$";
    public static final String MENSAJE_NOMBRE =
            "Solo minúsculas y números, separados por '.', '-' o '_' (ej: nexus.pagos.queue)";

    /** Palabras separadas por puntos; se permiten los comodines * y # de los exchanges topic. */
    public static final String ROUTING_KEY = "^([a-z0-9_-]+|\\*|#)(\\.([a-z0-9_-]+|\\*|#))*$";
    public static final String MENSAJE_ROUTING_KEY =
            "Palabras separadas por '.', con comodines * o # (ej: pago.aprobado, producto.#)";

    private Validaciones() {}
}