package com.epam.learn.song_service.service;

import com.epam.learn.song_service.entity.SongEntity;
import com.epam.learn.song_service.model.SongRequest;
import com.epam.learn.song_service.model.SongResponse;
import com.epam.learn.song_service.repository.SongRepository;
import com.epam.learn.song_service.service.mapper.SongMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SongService {

    private final SongMapper songMapper;
    private final SongRepository songRepository;

    public SongResponse uploadSong(SongRequest songRequest) {
        SongEntity songEntity = songMapper.toEntity(songRequest);

        songEntity = songRepository.save(songEntity);

        return SongResponse.builder().id(songEntity.getId()).build();
    }
}
