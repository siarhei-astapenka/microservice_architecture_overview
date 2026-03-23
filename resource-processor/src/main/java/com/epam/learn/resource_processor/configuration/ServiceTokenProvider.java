package com.epam.learn.resource_processor.configuration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class ServiceTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.service-token-expiration-ms:86400000}")
    private long serviceTokenExpirationMs;

    @Value("${jwt.service-token-subject:resource-processor}")
    private String serviceTokenSubject;

    private String serviceToken;

    @PostConstruct
    public void init() {
        generateServiceToken();
    }

    private void generateServiceToken() {
        try {
            serviceToken = Jwts.builder()
                .subject(serviceTokenSubject)
                .claim("roles", List.of("SERVICE"))
                .claim("scope", "SERVICE")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + serviceTokenExpirationMs))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
            log.info("Generated service token for resource-processor");
        } catch (Exception e) {
            log.error("Failed to generate service token", e);
        }
    }

    public String getServiceToken() {
        if (serviceToken == null) {
            generateServiceToken();
        }
        return serviceToken;
    }
}
