package com.backend.service;

import com.backend.dto.UsuarioRequest;
import com.backend.dto.UsuarioResponse;
import com.backend.model.UsuarioModel;
import com.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ==========================================
 * CAPA DE SERVICIO (LÓGICA DE NEGOCIO)
 * ==========================================
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Crea o actualiza el perfil a partir de los claims del token. */
    @Transactional
    public UsuarioResponse sincronizar(String azureOid, String nombre, String email, String rol) {
        UsuarioModel usuario = usuarioRepository.findByAzureOid(azureOid)
                .orElseGet(() -> UsuarioModel.builder().azureOid(azureOid).build());
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setRol(rol);
        return mapToResponse(usuarioRepository.save(usuario));
    }

    public List<UsuarioResponse> obtenerTodos() {
        return usuarioRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public Optional<UsuarioResponse> obtenerPorId(Long id) {
        return usuarioRepository.findById(id).map(this::mapToResponse);
    }

    @Transactional
    public Optional<UsuarioResponse> actualizar(Long id, UsuarioRequest request) {
        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setNombre(request.getNombre());
            return mapToResponse(usuarioRepository.save(usuario));
        });
    }

    @Transactional
    public boolean eliminar(Long id) {
        if (usuarioRepository.existsById(id)) {
            usuarioRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private UsuarioResponse mapToResponse(UsuarioModel u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .azureOid(u.getAzureOid())
                .nombre(u.getNombre())
                .email(u.getEmail())
                .rol(u.getRol())
                .build();
    }
}