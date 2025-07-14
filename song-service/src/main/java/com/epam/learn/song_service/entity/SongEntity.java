package com.epam.learn.song_service.entity;

import com.epam.learn.song_service.entity.converter.DurationConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDate;

@Entity
@Table(name = "metadata",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "resource_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SongEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_id", nullable = false, unique = true)
    private Long resourceId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false)
    private String album;

    @Column(nullable = false, columnDefinition = "SMALLINT")
    @Convert(converter = DurationConverter.class)
    private Duration duration;

    @Column(nullable = false)
    private LocalDate year;
}
