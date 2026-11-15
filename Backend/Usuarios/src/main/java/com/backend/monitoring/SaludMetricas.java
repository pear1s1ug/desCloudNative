package com.backend.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

/**
 * Gauge nexus_servicio_salud: 1 si /actuator/health esta UP, 0 si no (DOWN, OUT_OF_SERVICE...).
 * Prometheus lo usa para la alerta ServicioNoSaludable.
 */
@Component
public class SaludMetricas {

    public SaludMetricas(MeterRegistry registry, ObjectProvider<HealthEndpoint> healthEndpoint) {
        Gauge.builder("nexus.servicio.salud", () -> {
                    try {
                        HealthEndpoint endpoint = healthEndpoint.getIfAvailable();
                        if (endpoint == null) {
                            return Double.NaN;
                        }
                        return Status.UP.equals(endpoint.health().getStatus()) ? 1 : 0;
                    } catch (RuntimeException e) {
                        return 0;
                    }
                })
                .description("1 si el health del servicio esta UP, 0 si no")
                .register(registry);
    }
}