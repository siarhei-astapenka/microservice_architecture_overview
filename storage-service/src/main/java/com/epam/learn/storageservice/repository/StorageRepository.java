package com.epam.learn.storageservice.repository;

import com.epam.learn.storageservice.entity.StorageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StorageRepository extends JpaRepository<StorageEntity, Long> {
    Optional<StorageEntity> findByStorageType(String storageType);
    Optional<StorageEntity> findById(Long id);
}