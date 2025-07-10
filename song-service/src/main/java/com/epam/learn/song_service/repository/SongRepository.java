package com.epam.learn.song_service.repository;

import com.epam.learn.song_service.entity.SongEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository  extends JpaRepository<SongEntity, Long> {

}
