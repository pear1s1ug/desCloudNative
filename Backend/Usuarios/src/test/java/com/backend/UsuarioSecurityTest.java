package com.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioSecurityTest {

    private static final SimpleGrantedAuthority SCOPE = new SimpleGrantedAuthority("SCOPE_access_as_user");
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Admin");
    private static final SimpleGrantedAuthority CLIENTE = new SimpleGrantedAuthority("ROLE_Cliente");

    @Autowired
    private MockMvc mvc;

    @Test
    void sinToken_401() throws Exception {
        mvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void me_creaPerfilDesdeClaims_200() throws Exception {
        mvc.perform(get("/api/usuarios/me").with(jwt()
                        .jwt(j -> j.claim("oid", "oid-123")
                                .claim("name", "Ana Pérez")
                                .claim("preferred_username", "ana@test.cl")
                                .claim("roles", List.of("Cliente")))
                        .authorities(SCOPE, CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.azureOid").value("oid-123"))
                .andExpect(jsonPath("$.email").value("ana@test.cl"))
                .andExpect(jsonPath("$.rol").value("Cliente"));
    }

    @Test
    void cliente_noPuedeListar_403() throws Exception {
        mvc.perform(get("/api/usuarios").with(jwt().authorities(SCOPE, CLIENTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_lista_200() throws Exception {
        mvc.perform(get("/api/usuarios").with(jwt().authorities(SCOPE, ADMIN)))
                .andExpect(status().isOk());
    }
}
