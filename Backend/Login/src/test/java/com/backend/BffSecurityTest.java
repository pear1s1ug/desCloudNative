package com.backend;

import com.backend.service.DownstreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BffSecurityTest {

    private static final SimpleGrantedAuthority SCOPE = new SimpleGrantedAuthority("SCOPE_access_as_user");
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Admin");
    private static final SimpleGrantedAuthority CLIENTE = new SimpleGrantedAuthority("ROLE_Cliente");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DownstreamService downstream;

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenConScope_devuelve200() throws Exception {
        when(downstream.forward(any(), eq(HttpMethod.GET), eq("/api/productos"), any(), any()))
                .thenReturn(ResponseEntity.ok("[]"));

        mvc.perform(get("/api/productos").with(jwt().authorities(SCOPE)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenSinScope_devuelve403() throws Exception {
        mvc.perform(get("/api/productos").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void cliente_noPuedeListarUsuarios_403() throws Exception {
        mvc.perform(get("/api/usuarios").with(jwt().authorities(SCOPE, CLIENTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_puedeListarUsuarios_200() throws Exception {
        when(downstream.forward(any(), eq(HttpMethod.GET), eq("/api/usuarios"), any(), any()))
                .thenReturn(ResponseEntity.ok("[]"));

        mvc.perform(get("/api/usuarios").with(jwt().authorities(SCOPE, ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void me_devuelveClaimsDelToken() throws Exception {
        mvc.perform(get("/api/me").with(jwt()
                        .jwt(j -> j.claim("name", "Ana").claim("roles", List.of("Admin")))
                        .authorities(SCOPE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.roles[0]").value("Admin"));
    }
}