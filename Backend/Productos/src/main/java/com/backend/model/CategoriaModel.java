package com.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * ==========================================
 * CAPA DE MODELO (MODEL - MVC)
 * ==========================================
 * Entidad que representa la tabla 'categorias'.
 */
@Entity
@Table(name = "categorias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    private String descripcion;
}