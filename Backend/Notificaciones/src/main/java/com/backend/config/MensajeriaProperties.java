package com.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.messaging")
public record MensajeriaProperties(
    String exchangeDirect,
    String exchangeTopic,
    String exchangeDlx
) {}
