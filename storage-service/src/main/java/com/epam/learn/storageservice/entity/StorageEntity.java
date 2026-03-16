package com.epam.learn.storageservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "storages", uniqueConstraints = {
    @UniqueConstraint(name = "uk_storages_storage_type", columnNames = "storage_type"),
    @UniqueConstraint(name = "uk_storages_storage_bucket", columnNames = "storage_bucket")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "storage_type", nullable = false)
    private String storageType;

    @Column(name = "storage_bucket", nullable = false)
    private String bucket;

    @Column(name = "storage_path", nullable = false)
    private String path;
}