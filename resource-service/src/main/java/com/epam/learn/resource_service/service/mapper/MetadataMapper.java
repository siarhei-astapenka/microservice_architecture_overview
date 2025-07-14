package com.epam.learn.resource_service.service.mapper;

import com.epam.learn.resource_service.model.metadata.Metadata;
import com.epam.learn.resource_service.model.metadata.MetadataRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class MetadataMapper {
    public MetadataRequest toSongMetadataRequest(Metadata metadata, Long resourceId) {
        return MetadataRequest.builder()
                .id(resourceId)
                .name(metadata.getName())
                .artist(metadata.getArtist())
                .album(metadata.getAlbum())
                .duration(formatDuration(metadata.getDuration()))
                .year(formatYear(metadata.getYear()))
                .build();
    }

    private static String formatDuration(Duration duration) {
        return String.format("%02d:%02d",
                duration.toMinutesPart(),
                duration.toSecondsPart());
    }

    private static String formatYear(LocalDate yearDate) {
        return DateTimeFormatter.ofPattern("yyyy").format(yearDate);
    }
}
