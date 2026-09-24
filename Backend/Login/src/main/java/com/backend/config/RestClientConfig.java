package com.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Clientes HTTP hacia los microservicios internos.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient productosClient(@Value("${app.services.productos-url}") String url) {
        return RestClient.builder().baseUrl(url).build();
    }

    @Bean
    public RestClient usuariosClient(@Value("${app.services.usuarios-url}") String url) {
        return RestClient.builder().baseUrl(url).build();
    }
}