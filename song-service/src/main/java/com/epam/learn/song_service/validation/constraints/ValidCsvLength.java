package com.epam.learn.song_service.validation.constraints;

import com.epam.learn.song_service.validation.CsvLengthValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CsvLengthValidator.class)
public @interface ValidCsvLength {
    String message() default "Invalid CSV length";
    int max() default 200;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
