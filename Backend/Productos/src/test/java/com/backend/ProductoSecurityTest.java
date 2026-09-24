package com.backend;

import com.backend.model.CategoriaModel;
import com.backend.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductoSecurityTest {

    private static final SimpleGrantedAuthority SCOPE = new SimpleGrantedAuthority("SCOPE_access_as_user");
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Admin");
    private static final SimpleGrantedAuthority CLIENTE = new SimpleGrantedAuthority("ROLE_Cliente");

    private static final String PRODUCTO = """
            {"nombre":"Mouse","descripcion":"Test","precio":1000,"stock":5,"categoria":"Periféricos"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @BeforeEach
    void crearCategoria() {
        if (categoriaRepository.findByNombre("Periféricos").isEmpty()) {
            categoriaRepository.save(CategoriaModel.builder().nombre("Periféricos").build());
        }
    }

    @Test
    void sinToken_401() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isUnauthorized());
    }

    @Test
    void conScope_200() throws Exception {
        mvc.perform(get("/api/productos").with(jwt().authorities(SCOPE)))
                .andExpect(status().isOk());
    }

    @Test
    void sinScope_403() throws Exception {
        mvc.perform(get("/api/productos").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cliente_noPuedeCrear_403() throws Exception {
        mvc.perform(post("/api/productos").with(jwt().authorities(SCOPE, CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_crea_201() throws Exception {
        mvc.perform(post("/api/productos").with(jwt().authorities(SCOPE, ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isCreated());
    }

    @Test
    void categoriaInexistente_400() throws Exception {
        String malo = PRODUCTO.replace("Periféricos", "NoExiste");
        mvc.perform(post("/api/productos").with(jwt().authorities(SCOPE, ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(malo))
                .andExpect(status().isBadRequest());
    }
}