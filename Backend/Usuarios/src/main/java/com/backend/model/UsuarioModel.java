package com.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * ==========================================
 * CAPA DE MODELO (MODEL - MVC)
 * ==========================================
 * Entidad que representa la tabla 'usuarios'.
 * La contraseña la gestiona Azure AD; aquí solo se guarda el perfil.
 */
@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Object ID del usuario en Azure AD (claim "oid"). */
    @Column(name = "azure_oid", nullable = false, unique = true, length = 64)
    private String azureOid;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 150)
    private String email;

    /** Roles vigentes según el último token (claim "roles"). */
    @Column(nullable = false, length = 100)
    private String rol;
}