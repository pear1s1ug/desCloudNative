package com.backend.qa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Monitoreo: health, info y prometheus son publicos (GET); el resto de /actuator sigue cerrado. */
@SpringBootTest(properties = "management.prometheus.metrics.export.enabled=true")
@AutoConfigureMockMvc
class ActuatorMonitoreoTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void prometheus_sinToken_200_conMetricasDeKafka() throws Exception {
        MvcResult r = mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk()).andReturn();
        String body = r.getResponse().getContentAsString();
        assertThat(body).contains("nexus_servicio_salud", "nexus_kafka_broker_disponible", "application=\"");
        
    }

    @Test
    void health_sinToken_noPideJwt_eIncluyeKafka() throws Exception {
        MvcResult r = mvc.perform(get("/actuator/health")).andReturn();
        // 200 si Kafka y el resto estan UP, 503 si algo esta DOWN (por ejemplo, sin broker): nunca 401/403
        assertThat(r.getResponse().getStatus()).isIn(200, 503);
        assertThat(r.getResponse().getContentAsString()).contains("\"kafka\"");
    }

    @Test
    void probes_e_info_sinToken() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/info")).andExpect(status().isOk());
    }

    @Test
    void endpointsNoExpuestos_sinToken_401() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/beans")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    void endpointsNoExpuestos_conAdmin_404() throws Exception {
        mvc.perform(get("/actuator/env").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isNotFound());
    }
}