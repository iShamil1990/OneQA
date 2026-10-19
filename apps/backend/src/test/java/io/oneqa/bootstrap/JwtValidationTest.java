package io.oneqa.bootstrap;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtValidationTest {
    private static final String ISSUER = "https://identity.example.test/realm";
    private static final String AUDIENCE = "oneqa-api";

    private Jwt token(String issuer, List<String> audiences, Instant expiresAt) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .issuer(issuer)
            .subject(UUID.randomUUID().toString())
            .audience(audiences)
            .issuedAt(now.minusSeconds(60))
            .expiresAt(expiresAt)
            .build();
    }

    @Test
    void acceptsCorrectIssuerAndAudience() {
        assertFalse(SecurityConfiguration.tokenValidator(ISSUER, AUDIENCE)
            .validate(token(ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(600))).hasErrors());
    }

    @Test
    void rejectsWrongIssuer() {
        assertTrue(SecurityConfiguration.tokenValidator(ISSUER, AUDIENCE)
            .validate(token("https://attacker.example.test", List.of(AUDIENCE), Instant.now().plusSeconds(600))).hasErrors());
    }

    @Test
    void rejectsWrongAudience() {
        assertTrue(SecurityConfiguration.tokenValidator(ISSUER, AUDIENCE)
            .validate(token(ISSUER, List.of("other-api"), Instant.now().plusSeconds(600))).hasErrors());
    }

    @Test
    void rejectsExpiredToken() {
        assertTrue(SecurityConfiguration.tokenValidator(ISSUER, AUDIENCE)
            .validate(token(ISSUER, List.of(AUDIENCE), Instant.now().minusSeconds(600))).hasErrors());
    }
}
