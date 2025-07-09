package com.epam.learn.resource_service.validation;

import com.epam.learn.resource_service.validation.constraints.ValidMP3;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import java.io.ByteArrayInputStream;

public class MP3FileValidator implements ConstraintValidator<ValidMP3, byte[]> {

    @Override
    public boolean isValid(byte[] file, ConstraintValidatorContext context) {
        if (file == null || file.length == 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("File cannot be empty")
                    .addConstraintViolation();
            return false;
        }

        try {
            Metadata metadata = new Metadata();

            new AutoDetectParser().parse(
                    new ByteArrayInputStream(file),
                    new BodyContentHandler(),
                    metadata,
                    new ParseContext()
            );

            String mimeType = metadata.get(Metadata.CONTENT_TYPE);

            if (!"audio/mpeg".equals(mimeType)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Invalid file format. Only MP3 files are allowed")
                        .addConstraintViolation();
                return false;
            }
            return true;
        } catch (Exception e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Invalid file format. Only MP3 files are allowed")
                    .addConstraintViolation();
            return false;
        }
    }
}
