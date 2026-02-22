package com.epam.learn.resource_service.validation;

import com.epam.learn.resource_service.validation.constraints.ValidCsvLength;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CsvLengthValidator Unit Tests")
class CsvLengthValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should be valid when CSV string is within default limit (200 chars)")
    void isValid_withinDefaultLimit() {
        // Given
        String csv = "1,2,3,4,5"; // 9 characters

        // When
        boolean result = validator.validate(new TestEntity(csv)).isEmpty();

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should be valid when CSV string is exactly at limit")
    void isValid_atLimit() {
        // Given - create string with exactly 200 characters
        String csv = "a".repeat(200);

        // When
        boolean result = validator.validate(new TestEntity(csv)).isEmpty();

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should be invalid when CSV string exceeds default limit")
    void isValid_exceedsDefaultLimit() {
        // Given - create string with 201 characters
        String csv = "a".repeat(201);

        // When
        Set<ConstraintViolation<TestEntity>> violations = validator.validate(new TestEntity(csv));

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
            v.getMessage().contains("CSV string is too long") &&
            v.getMessage().contains("201") &&
            v.getMessage().contains("200")
        ));
    }

    @Test
    @DisplayName("Should be valid when CSV string is null")
    void isValid_nullString() {
        // Given
        String csv = null;

        // When
        boolean result = validator.validate(new TestEntity(csv)).isEmpty();

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should be valid when CSV string is empty")
    void isValid_emptyString() {
        // Given
        String csv = "";

        // When
        boolean result = validator.validate(new TestEntity(csv)).isEmpty();

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should use custom max length from annotation")
    void isValid_customMaxLength() {
        // Given - create string with 10 characters, but max is 5
        String csv = "a,b,c,d,e,f"; // 11 characters

        // When
        Set<ConstraintViolation<TestEntityWithCustomMax>> violations = 
            validator.validate(new TestEntityWithCustomMax(csv));

        // Then
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Should be valid for string at custom max length")
    void isValid_atCustomMaxLength() {
        // Given - create string with 5 characters, max is 5
        String csv = "a,b,c"; // 5 characters

        // When
        boolean result = validator.validate(new TestEntityWithCustomMax(csv)).isEmpty();

        // Then
        assertTrue(result);
    }

    // Test helper class with default max (200)
    static class TestEntity {
        @ValidCsvLength
        private String csv;

        public TestEntity(String csv) {
            this.csv = csv;
        }

        public String getCsv() {
            return csv;
        }
    }

    // Test helper class with custom max (5)
    static class TestEntityWithCustomMax {
        @ValidCsvLength(max = 5)
        private String csv;

        public TestEntityWithCustomMax(String csv) {
            this.csv = csv;
        }

        public String getCsv() {
            return csv;
        }
    }
}
