package com.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * ==========================================
 * CAPA DE MODELO (MODEL - MVC)
 * ==========================================
 * Entidad que representa la tabla 'productos'.
 * Cada producto pertenece a una categoría (FK categoria_id).
 */
@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false)
    private Double precio;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaModel categoria;
}