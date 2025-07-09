package com.epam.learn.song_service.validation;

import com.epam.learn.song_service.validation.constraints.ValidSongDuration;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Duration;

public class SongDurationValidator implements ConstraintValidator<ValidSongDuration, Duration> {
    @Override
    public void initialize(ValidSongDuration constraintAnnotation) {
        // Initialization if needed
    }

    @Override
    public boolean isValid(Duration duration, ConstraintValidatorContext context) {
        if (duration == null) {
            return false;
        }

        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        boolean valid = minutes >= 0 && minutes < 60 && seconds >= 0 && seconds < 60;

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Duration must represent mm:ss format (00:00 to 59:59)"
            ).addConstraintViolation();
        }

        return valid;
    }
}

