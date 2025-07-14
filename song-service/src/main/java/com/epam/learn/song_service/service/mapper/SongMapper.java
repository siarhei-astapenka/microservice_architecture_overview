package com.epam.learn.song_service.service.mapper;

import com.epam.learn.song_service.entity.SongEntity;
import com.epam.learn.song_service.model.SongMetadataRequest;
import com.epam.learn.song_service.model.SongMetadataResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class SongMapper {
    public SongEntity toEntity(SongMetadataRequest request) {
        return SongEntity.builder()
                .resourceId(request.getId())
                .name(request.getName())
                .artist(request.getArtist())
                .album(request.getAlbum())
                .duration(parseDuration(request.getDuration()))
                .year(parseYear(request.getYear()))
                .build();
    }

    public SongMetadataResponse toResponse(SongEntity entity) {
        return SongMetadataResponse.builder()
                .id(entity.getResourceId())
                .name(entity.getName())
                .artist(entity.getArtist())
                .album(entity.getAlbum())
                .duration(formatDuration(entity.getDuration()))
                .year(formatYear(entity.getYear()))
                .build();
    }

    private static Duration parseDuration(String mmss) {
        String[] parts = mmss.split(":");
        return Duration.ofMinutes(Long.parseLong(parts[0]))
                .plusSeconds(Long.parseLong(parts[1]));
    }

    private static LocalDate parseYear(String yearStr) {
        return LocalDate.of(Integer.parseInt(yearStr), 1, 1);
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
