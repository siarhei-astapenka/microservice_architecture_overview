package com.epam.learn.resource_service.service.parser;

import com.epam.learn.resource_service.exception.BadRequestException;
import com.epam.learn.resource_service.model.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
public class MetadataParser {
    public Optional<Metadata> getSongMetadataFromFile(byte[] file) {
        if (file == null || file.length == 0) {
            return Optional.empty();
        }

        try (InputStream input = new ByteArrayInputStream(file)) {
            org.apache.tika.metadata.Metadata metadata = extractMetadata(input);
            return Optional.of(buildSongMetadata(metadata));
        } catch (Exception e) {
            throw new BadRequestException("Invalid metadata file format");
        }
    }

    private org.apache.tika.metadata.Metadata extractMetadata(InputStream input) throws Exception {
        BodyContentHandler handler = new BodyContentHandler();
        org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();
        ParseContext context = new ParseContext();

        new Mp3Parser().parse(input, handler, metadata, context);
        return metadata;
    }

    private Metadata buildSongMetadata(org.apache.tika.metadata.Metadata metadata) {
        return Metadata.builder()
                .name(getSafeMetadataValue(metadata, "title"))
                .artist(getSafeMetadataValue(metadata, "xmpDM:artist"))
                .album(getSafeMetadataValue(metadata, "xmpDM:album"))
                .duration(parseDuration(metadata.get("xmpDM:duration")))
                .year(parseReleaseDate(metadata.get("xmpDM:releaseDate")))
                .build();
    }

    private String getSafeMetadataValue(org.apache.tika.metadata.Metadata metadata, String key) {
        String value = metadata.get(key);
        return value != null ? value.trim() : null;
    }

    private Duration parseDuration(String durationString) {
        if (durationString == null || durationString.trim().isEmpty()) {
            return null;
        }

        try {
            double seconds = Double.parseDouble(durationString.trim());
            return Duration.ofMillis((long) (seconds * 1000));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid duration format: " + durationString);
        }
    }

    private LocalDate parseReleaseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        try {
            String normalizedDate = dateString.trim();

            try {
                return LocalDate.parse(normalizedDate);
            } catch (DateTimeParseException e) {
                if (normalizedDate.matches("\\d{4}")) {
                    return LocalDate.ofYearDay(Integer.parseInt(normalizedDate), 1);
                }
                throw e;
            }
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format: " + dateString);
        }
    }
}
