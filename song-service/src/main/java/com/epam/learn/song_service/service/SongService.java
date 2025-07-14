package com.epam.learn.song_service.service;

import com.epam.learn.song_service.entity.SongEntity;
import com.epam.learn.song_service.exception.ConflictException;
import com.epam.learn.song_service.exception.NotFoundException;
import com.epam.learn.song_service.model.SongRequest;
import com.epam.learn.song_service.model.SongResponse;
import com.epam.learn.song_service.repository.SongRepository;
import com.epam.learn.song_service.service.mapper.SongMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SongService {

    private final SongMapper songMapper;
    private final SongRepository songRepository;

    @Transactional
    public SongResponse saveSongMetadata(SongRequest songRequest) {
        SongEntity songEntity = songMapper.toEntity(songRequest);

        songRepository.findByResourceId(songEntity.getResourceId())
                .ifPresent(existing -> {
                    throw new ConflictException(
                            String.format("Metadata for resource ID=%s already exists", songEntity.getResourceId()),
                            String.valueOf(HttpStatus.CONFLICT.value())
                    );
                });

        return SongResponse.builder()
                .id(songRepository.save(songEntity).getId())
                .build();
    }

    public SongResponse getSongMetadataByResourceId(Long resourceId) {
        return songMapper.toResponse(
                songRepository.findByResourceId(resourceId)
                        .orElseThrow(() -> new NotFoundException(String.format("Resource with ID=%s not found", resourceId))));
    }

    public Map<String, List<Long>> deleteSongMetadata(String ids) {
        Map<String, List<Long>> response = new HashMap<>();

        List<Long> idsToDelete = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Long> existingIds = songRepository.findExistingIds(idsToDelete);

        songRepository.deleteAllById(existingIds);

        response.put("ids", existingIds);

        return response;
    }
}
