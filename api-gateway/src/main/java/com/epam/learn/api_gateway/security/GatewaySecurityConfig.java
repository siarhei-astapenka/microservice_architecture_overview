package com.epam.learn.api_gateway.security;

import com.epam.learn.api_gateway.error.ForbiddenException;
import com.epam.learn.api_gateway.error.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/auth/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/error").permitAll()
                .pathMatchers("/health/**").permitAll()
                .pathMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.POST, "/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.GET, "/resources/**").hasAnyRole("USER", "ADMIN")
                .pathMatchers(HttpMethod.GET, "/songs/**").hasAnyRole("USER", "ADMIN")
                .anyExchange().authenticated()
            )
            .addFilterAt(jwtAuthenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                .accessDeniedHandler(new CustomAccessDeniedHandler())
            )
            .build();
    }

    @Bean
    public AuthenticationWebFilter jwtAuthenticationWebFilter() {
        AuthenticationWebFilter filter = new AuthenticationWebFilter(reactiveAuthenticationManager());
        filter.setServerAuthenticationConverter(new JwtAuthenticationConverter(jwtSecret));
        filter.setAuthenticationFailureHandler(new ServerAuthenticationEntryPointFailureHandler(
            new CustomAuthenticationEntryPoint()));
        return filter;
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return new JwtReactiveAuthenticationManager();
    }

    public static class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
        @Override
        public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException authException) {
            return Mono.error(new UnauthorizedException("Invalid or missing authentication token"));
        }
    }

    public static class CustomAccessDeniedHandler implements org.springframework.security.web.server.authorization.ServerAccessDeniedHandler {
        @Override
        public Mono<Void> handle(ServerWebExchange exchange, org.springframework.security.access.AccessDeniedException denied) {
            return Mono.error(new ForbiddenException("You do not have permission to access this resource"));
        }
    }

    public static class JwtAuthenticationConverter implements ServerAuthenticationConverter {

        private final String secret;

        public JwtAuthenticationConverter(String secret) {
            this.secret = secret;
        }

        @Override
        public Mono<Authentication> convert(ServerWebExchange exchange) {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Mono.empty();
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

                String username = claims.getSubject();

                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);
                if (roles == null) {
                    roles = new ArrayList<>();
                }

                List<GrantedAuthority> authorities = roles.stream()
                    .map(String::valueOf)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .map(GrantedAuthority.class::cast)
                    .toList();

                return Mono.just(new UsernamePasswordAuthenticationToken(username, null, authorities));
            } catch (Exception e) {
                return Mono.error(new BadCredentialsException("Invalid authentication token", e));
            }
        }
    }

    public static class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

        @Override
        public Mono<Authentication> authenticate(Authentication authentication) {
            if (authentication instanceof UsernamePasswordAuthenticationToken) {
                return Mono.just(authentication);
            }
            return Mono.empty();
        }
    }
}
