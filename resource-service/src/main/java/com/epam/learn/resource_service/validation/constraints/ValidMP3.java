package com.epam.learn.resource_service.validation.constraints;

import com.epam.learn.resource_service.validation.MP3FileValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Constraint(validatedBy = MP3FileValidator.class)
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMP3 {
    String message() default "Invalid MP3 file";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
