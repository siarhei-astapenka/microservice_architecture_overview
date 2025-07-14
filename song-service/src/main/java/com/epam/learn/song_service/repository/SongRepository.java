package com.epam.learn.song_service.repository;

import com.epam.learn.song_service.entity.SongEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SongRepository  extends JpaRepository<SongEntity, Long> {
    @Query("SELECT s.resourceId FROM SongEntity s WHERE s.resourceId IN :ids")
    List<Long> findExistingIds(@Param("ids") Collection<Long> ids);
    Optional<SongEntity> findByResourceId(Long resourceId);
}
