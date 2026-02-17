package com.epam.learn.song_service.repository;

import com.epam.learn.song_service.entity.SongEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SongRepository Integration Tests")
class SongRepositoryIntegrationTest {

    @Autowired
    private SongRepository songRepository;

    @Test
    @DisplayName("Should persist and load SongEntity including converter fields")
    void saveAndFindByResourceId_shouldPersistAndLoad() {
        SongEntity saved = songRepository.saveAndFlush(SongEntity.builder()
                .resourceId(100L)
                .name("Test Song")
                .artist("Test Artist")
                .album("Test Album")
                .duration(Duration.ofMinutes(3).plusSeconds(30))
                .year(LocalDate.of(2024, 1, 1))
                .build());

        SongEntity found = songRepository.findByResourceId(100L).orElseThrow();

        assertNotNull(saved.getId());
        assertEquals(saved.getId(), found.getId());
        assertEquals(100L, found.getResourceId());
        assertEquals("Test Song", found.getName());
        assertEquals("Test Artist", found.getArtist());
        assertEquals("Test Album", found.getAlbum());
        assertEquals(Duration.ofMinutes(3).plusSeconds(30), found.getDuration());
        assertEquals(LocalDate.of(2024, 1, 1), found.getYear());
    }

    @Test
    @DisplayName("Should return only existing resource IDs")
    void findExistingIds_shouldReturnOnlyExisting() {
        songRepository.saveAllAndFlush(List.of(
                SongEntity.builder()
                        .resourceId(1L)
                        .name("A")
                        .artist("A")
                        .album("A")
                        .duration(Duration.ofSeconds(30))
                        .year(LocalDate.of(2020, 1, 1))
                        .build(),
                SongEntity.builder()
                        .resourceId(3L)
                        .name("B")
                        .artist("B")
                        .album("B")
                        .duration(Duration.ofSeconds(31))
                        .year(LocalDate.of(2021, 1, 1))
                        .build()
        ));

        List<Long> existing = songRepository.findExistingIds(List.of(1L, 2L, 3L, 999L));

        assertEquals(2, existing.size());
        assertTrue(existing.containsAll(List.of(1L, 3L)));
        assertFalse(existing.contains(2L));
        assertFalse(existing.contains(999L));
    }

    @Test
    @DisplayName("Should delete by resourceId list")
    void deleteAllByResourceIdIn_shouldDelete() {
        songRepository.saveAllAndFlush(List.of(
                SongEntity.builder()
                        .resourceId(10L)
                        .name("A")
                        .artist("A")
                        .album("A")
                        .duration(Duration.ofSeconds(30))
                        .year(LocalDate.of(2020, 1, 1))
                        .build(),
                SongEntity.builder()
                        .resourceId(20L)
                        .name("B")
                        .artist("B")
                        .album("B")
                        .duration(Duration.ofSeconds(31))
                        .year(LocalDate.of(2021, 1, 1))
                        .build()
        ));

        songRepository.deleteAllByResourceIdIn(List.of(10L));
        songRepository.flush();

        assertTrue(songRepository.findByResourceId(10L).isEmpty());
        assertTrue(songRepository.findByResourceId(20L).isPresent());
    }

    @Test
    @DisplayName("Should enforce unique resourceId constraint")
    void save_shouldEnforceUniqueResourceId() {
        songRepository.saveAndFlush(SongEntity.builder()
                .resourceId(42L)
                .name("A")
                .artist("A")
                .album("A")
                .duration(Duration.ofSeconds(30))
                .year(LocalDate.of(2020, 1, 1))
                .build());

        assertThrows(DataIntegrityViolationException.class, () -> songRepository.saveAndFlush(SongEntity.builder()
                .resourceId(42L)
                .name("B")
                .artist("B")
                .album("B")
                .duration(Duration.ofSeconds(31))
                .year(LocalDate.of(2021, 1, 1))
                .build()));
    }
}
