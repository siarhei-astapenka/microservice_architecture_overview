package com.epam.learn.resource_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class StorageConnectionException extends RuntimeException {
    public StorageConnectionException(String message) {
        super(message);
    }

    public StorageConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
