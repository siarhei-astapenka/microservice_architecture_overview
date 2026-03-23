package com.epam.learn.api_gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorMessage = "Unexpected error";

        if (ex instanceof UnauthorizedException) {
            status = HttpStatus.UNAUTHORIZED;
            errorMessage = ex.getMessage();
        } else if (ex instanceof ForbiddenException) {
            status = HttpStatus.FORBIDDEN;
            errorMessage = ex.getMessage();
        } else if (ex instanceof NotFoundException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorMessage = "Service unavailable";
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            errorMessage = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(String.valueOf(status.value()))
                .errorMessage(errorMessage)
                .path(exchange.getRequest().getURI().getPath())
                .build();

        String json = toJson(errorResponse);

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(
                response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))));
    }

    private String toJson(ErrorResponse errorResponse) {
        try {
            return objectMapper.writeValueAsString(errorResponse);
        } catch (Exception e) {
            return "{\"errorCode\":\"" + errorResponse.getErrorCode() + 
                   "\",\"errorMessage\":\"" + errorResponse.getErrorMessage() + 
                   "\",\"path\":\"" + errorResponse.getPath() + "\"}";
        }
    }
}
