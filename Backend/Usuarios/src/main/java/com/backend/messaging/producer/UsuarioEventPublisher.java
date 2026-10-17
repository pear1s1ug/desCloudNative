package com.backend.messaging.producer;

public interface UsuarioEventPublisher {

    void usuarioRegistrado(String azureOid, String nombre, String email);
}