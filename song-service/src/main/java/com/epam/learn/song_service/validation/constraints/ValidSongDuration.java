package com.epam.learn.song_service.validation.constraints;

import com.epam.learn.song_service.validation.SongDurationValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SongDurationValidator.class)
public @interface ValidSongDuration {
    String message() default "Duration must be in mm:ss format with leading zeros";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
