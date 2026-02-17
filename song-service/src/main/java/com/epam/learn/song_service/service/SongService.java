package com.epam.learn.song_service.service;

import com.epam.learn.song_service.entity.SongEntity;
import com.epam.learn.song_service.exception.ConflictException;
import com.epam.learn.song_service.exception.NotFoundException;
import com.epam.learn.song_service.model.SongMetadataRequest;
import com.epam.learn.song_service.model.SongMetadataResponse;
import com.epam.learn.song_service.repository.SongRepository;
import com.epam.learn.song_service.service.mapper.SongMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SongService {

    private final SongMapper songMapper;
    private final SongRepository songRepository;

    public SongMetadataResponse saveSongMetadata(SongMetadataRequest songMetadataRequest) {
        SongEntity songEntity = songMapper.toEntity(songMetadataRequest);

        songRepository.findByResourceId(songEntity.getResourceId())
                .ifPresent(existing -> {
                    log.warn("Metadata for resourceId={} already exists. Conflict detected.", songEntity.getResourceId());
                    throw new ConflictException(
                            String.format("Metadata for resource ID=%s already exists", songEntity.getResourceId()),
                            String.valueOf(HttpStatus.CONFLICT.value())
                    );
                });

        SongEntity savedEntity = songRepository.save(songEntity);
        log.info("Saved song metadata with resourceId={} and metadata id={}", savedEntity.getResourceId(), savedEntity.getId());
        
        return SongMetadataResponse.builder()
                .id(savedEntity.getId())
                .build();
    }

    public SongMetadataResponse getSongMetadataByResourceId(Long resourceId) {
        log.debug("Searching for song metadata with resourceId: {}", resourceId);
        
        return songMapper.toResponse(
                songRepository.findByResourceId(resourceId)
                        .orElseThrow(() -> {
                            log.warn("Song metadata not found for resourceId: {}", resourceId);
                            return new NotFoundException(String.format("Resource with ID=%s not found", resourceId));
                        })
        );
    }

    @Transactional
    public Map<String, List<Long>> deleteSongMetadata(String ids) {
        Map<String, List<Long>> response = new HashMap<>();

        List<Long> idsToDelete = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Long> resourceIds = songRepository.findExistingIds(idsToDelete);
        
        if (resourceIds.isEmpty()) {
            log.warn("No existing song metadata found for deletion among requested IDs: {}", idsToDelete);
        } else {
            log.info("Found {} existing song(s) to delete", resourceIds.size());
        }

        songRepository.deleteAllByResourceIdIn(resourceIds);

        log.info("Successfully deleted song metadata for resourceIds: {}", resourceIds);
        response.put("ids", resourceIds);

        return response;
    }
}
