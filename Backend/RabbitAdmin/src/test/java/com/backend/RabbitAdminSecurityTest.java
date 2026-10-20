package com.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seguridad y validación de la API de administración de RabbitMQ. */
@SpringBootTest
@AutoConfigureMockMvc
class RabbitAdminSecurityTest {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Admin");
    private static final SimpleGrantedAuthority CLIENTE = new SimpleGrantedAuthority("ROLE_Cliente");
    private static final String BASE = "/api/admin/rabbit";

    @Autowired
    private MockMvc mvc;

    @Test
    void sinToken_401() throws Exception {
        mvc.perform(post(BASE + "/queues")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"demo.queue\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cliente_403() throws Exception {
        mvc.perform(post(BASE + "/queues").with(jwt().authorities(CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"demo.queue\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_nombreVacio_400() throws Exception {
        mvc.perform(post(BASE + "/queues").with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    void admin_ttlNegativo_400() throws Exception {
        mvc.perform(post(BASE + "/queues").with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"demo.queue\",\"ttlMs\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_tipoExchangeInvalido_400() throws Exception {
        mvc.perform(post(BASE + "/exchanges").with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"demo.exchange\",\"tipo\":\"HEADERS_X\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_nombreInvalidoEnRuta_400() throws Exception {
        mvc.perform(get(BASE + "/queues/Cola Invalida!").with(jwt().authorities(ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_nombreReservado_409() throws Exception {
        mvc.perform(post(BASE + "/queues").with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"amq.prueba\"}"))
                .andExpect(status().isConflict());
    }
}