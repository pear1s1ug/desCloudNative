package com.backend;

import com.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtValidationTest {

    private static final String ISSUER = "https://login.microsoftonline.com/tenant/v2.0";
    private static final String AUDIENCE = "client-id";

    private final OAuth2TokenValidator<Jwt> validator = SecurityConfig.tokenValidator(ISSUER, List.of(AUDIENCE));

    private Jwt token(String iss, String aud, Instant exp) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(iss)
                .audience(List.of(aud))
                .issuedAt(exp.minusSeconds(3600))
                .expiresAt(exp)
                .build();
    }

    @Test
    void tokenValido() {
        assertFalse(validator.validate(token(ISSUER, AUDIENCE, Instant.now().plusSeconds(300))).hasErrors());
    }

    @Test
    void audienceIncorrecta() {
        assertTrue(validator.validate(token(ISSUER, "otra-api", Instant.now().plusSeconds(300))).hasErrors());
    }

    @Test
    void issuerIncorrecto() {
        assertTrue(validator.validate(token("https://evil.com/v2.0", AUDIENCE, Instant.now().plusSeconds(300))).hasErrors());
    }

    @Test
    void tokenExpirado() {
        assertTrue(validator.validate(token(ISSUER, AUDIENCE, Instant.now().minusSeconds(600))).hasErrors());
    }
}