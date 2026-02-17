package com.epam.learn.resource_service.validation;

import com.epam.learn.resource_service.validation.constraints.ValidMP3;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MP3FileValidator.
 * Tests file validation using real test MP3 files from test resources.
 */
@DisplayName("MP3FileValidator Unit Tests")
class MP3FileValidatorTest {

    private MP3FileValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new MP3FileValidator();
        validator.initialize(new ValidMP3() {
            @Override
            public String message() {
                return "default message";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() {
                @SuppressWarnings("unchecked")
                Class<? extends jakarta.validation.Payload>[] payload = new Class[0];
                return payload;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return ValidMP3.class;
            }
        });
        
        context = mock(ConstraintValidatorContext.class);
        violationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        
        when(context.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(violationBuilder);
    }

    @Test
    @DisplayName("Should be invalid when file is null")
    void isValid_nullFile() {
        // Given
        byte[] file = null;

        // When
        boolean result = validator.isValid(file, context);

        // Then
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Should be invalid when file is empty")
    void isValid_emptyFile() {
        // Given
        byte[] file = new byte[0];

        // When
        boolean result = validator.isValid(file, context);

        // Then
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Should validate valid MP3 file with required tags")
    void isValid_validMp3File() throws IOException {
        // Given
        Path validMp3Path = new ClassPathResource("testdata/valid-sample-with-required-tags.mp3").getFile().toPath();
        byte[] validMp3Bytes = Files.readAllBytes(validMp3Path);

        // When
        boolean result = validator.isValid(validMp3Bytes, context);

        // Then - valid MP3 should pass
        assertTrue(result);
    }

    @Test
    @DisplayName("Should validate MP3 file with missed tags")
    void isValid_missedTagsMp3File() throws IOException {
        // Given
        Path invalidMp3Path = new ClassPathResource("testdata/invalid-sample-with-missed-tags.mp3").getFile().toPath();
        byte[] invalidMp3Bytes = Files.readAllBytes(invalidMp3Path);

        // When
        boolean result = validator.isValid(invalidMp3Bytes, context);

        // Then - file with missed tags is still valid MP3 (just missing metadata)
        assertTrue(result);
    }
}
