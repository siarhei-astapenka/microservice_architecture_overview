package com.epam.learn.resource_service.entity;

import com.epam.learn.resource_service.enumeration.ResourceState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Storage key - just the filename (e.g., UUID.mp3), no path
    // Path is obtained from Storage Service based on state at runtime
    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    // Processing state: STAGING or PERMANENT
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    @Builder.Default
    private ResourceState state = ResourceState.STAGING;
}
