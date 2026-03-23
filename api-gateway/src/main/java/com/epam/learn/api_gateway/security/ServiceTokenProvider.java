package com.epam.learn.api_gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class ServiceTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.service-token-expiration-ms:86400000}")
    private long serviceTokenExpirationMs;

    @Value("${jwt.service-token-subject:api-gateway}")
    private String serviceTokenSubject;

    private volatile String serviceToken;
    private volatile Instant expiresAt;

    @PostConstruct
    public void init() {
        generateServiceToken();
    }

    public synchronized String getServiceToken() {
        if (serviceToken == null || isExpiringSoon()) {
            generateServiceToken();
        }
        return serviceToken;
    }

    private boolean isExpiringSoon() {
        return expiresAt == null || Instant.now().isAfter(expiresAt.minus(5, ChronoUnit.MINUTES));
    }

    private void generateServiceToken() {
        try {
            Instant issuedAt = Instant.now();
            Instant expiration = issuedAt.plusMillis(serviceTokenExpirationMs);

            serviceToken = Jwts.builder()
                .subject(serviceTokenSubject)
                .claim("roles", List.of("SERVICE"))
                .claim("scope", "SERVICE")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

            expiresAt = expiration;
            log.info("Generated service token for api-gateway (expires at {})", expiresAt);
        } catch (Exception e) {
            log.error("Failed to generate service token", e);
            serviceToken = null;
            expiresAt = null;
        }
    }
}
