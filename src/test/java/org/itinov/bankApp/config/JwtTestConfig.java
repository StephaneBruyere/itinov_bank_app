package org.itinov.bankApp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class JwtTestConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        // Dummy JwtDecoder, never really verifying tokens in tests
        // On renvoie un "fake" JWT dès qu’un token est reçu
        return token -> Jwt.withTokenValue(token)
            .header("alg", "none")
            .claim("sub", "11111111-1111-1111-1111-111111111111")
            .claim("preferred_username", "jane")
            .claim("realm_access", Map.of("roles", List.of("customer")))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }
}
