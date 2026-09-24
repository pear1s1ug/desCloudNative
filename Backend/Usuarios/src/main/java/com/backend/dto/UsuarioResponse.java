package com.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ==========================================
 * CAPA DE VISTA / DTO (DATA TRANSFER OBJECT)
 * ==========================================
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {

    private Long id;
    private String azureOid;
    private String nombre;
    private String email;
    private String rol;
}