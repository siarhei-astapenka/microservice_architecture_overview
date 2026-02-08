package com.epam.learn.resource_processor.service.mapper;

import com.epam.learn.resource_processor.model.metadata.Metadata;
import com.epam.learn.resource_processor.dto.SongMetadataRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class MetadataMapper {
    public SongMetadataRequest toSongMetadataRequest(Metadata metadata, Long resourceId) {
        return SongMetadataRequest.builder()
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
