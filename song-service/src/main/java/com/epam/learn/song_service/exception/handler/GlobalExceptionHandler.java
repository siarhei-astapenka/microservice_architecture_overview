package com.epam.learn.song_service.exception.handler;

import com.epam.learn.song_service.exception.ConflictException;
import com.epam.learn.song_service.exception.NotFoundException;
import com.epam.learn.song_service.model.ErrorResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .errorMessage("Validation error")
                        .details(errors)
                        .errorCode(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .build()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations()
                .stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("Validation failed");

        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .errorMessage(errorMessage)
                        .errorCode(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .build()
        );
    }

    @ExceptionHandler(JsonMappingException.class)
    public ResponseEntity<ErrorResponse> handleJsonMappingException(JsonMappingException ex) {
        String fieldName = ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("unknown");

        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .errorMessage("Validation error")
                        .details(Map.of(fieldName, ex.getOriginalMessage()))
                        .errorCode(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .build()
        );
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .errorCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                        .errorMessage(ex.getMessage())
                        .build()
                );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .errorMessage(ex.getMessage())
                        .errorCode(ex.getErrorCode())
                        .build()
                );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .errorMessage("Resource already exists")
                        .errorCode(String.valueOf(HttpStatus.CONFLICT.value()))
                        .build()
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleExceptions() {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.builder()
                        .errorMessage(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                        .errorCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                        .build()
                );
    }
}