package com.epam.learn.api_gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ServiceTokenFilter implements GlobalFilter, Ordered {

    private final ServiceTokenProvider serviceTokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!requiresServiceToken(path)) {
            return chain.filter(exchange);
        }

        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(builder -> builder
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION,
                    "Bearer " + serviceTokenProvider.getServiceToken())))
            .build();

        return chain.filter(mutatedExchange);
    }

    private boolean requiresServiceToken(String path) {
        return path.startsWith("/resources") || path.startsWith("/songs");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
