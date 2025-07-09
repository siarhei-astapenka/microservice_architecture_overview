package com.epam.learn.resource_service.validation;

import com.epam.learn.resource_service.validation.constraints.ValidCsvLength;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CsvLengthValidator implements ConstraintValidator<ValidCsvLength, String> {
    private int maxLength;

    @Override
    public void initialize(ValidCsvLength constraintAnnotation) {
        this.maxLength = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;

        if (value.length() > maxLength) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            String.format("CSV string is too long: received %d characters, maximum allowed is %d",
                                    value.length(), maxLength))
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
