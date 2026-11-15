package com.backend.dto;

/** Reglas de formato compartidas por los DTOs de administración. */
public final class Validaciones {

    /** Minúsculas y números, separados por un punto, guion o guion bajo. Permite prefijo interno __. */
    public static final String NOMBRE = "^(__)?[a-z0-9]+([._-][a-z0-9]+)*$";
    public static final String MENSAJE_NOMBRE =
            "Solo minúsculas y números, separados por '.', '-' o '_' (ej: nexus.pagos.topic)";

    private Validaciones() {}
}
