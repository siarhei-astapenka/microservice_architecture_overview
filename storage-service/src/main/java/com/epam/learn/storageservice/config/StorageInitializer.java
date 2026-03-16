package com.epam.learn.storageservice.config;

import com.epam.learn.storageservice.entity.StorageEntity;
import com.epam.learn.storageservice.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageInitializer {

    private static final Logger logger = LoggerFactory.getLogger(StorageInitializer.class);

    @Bean
    CommandLineRunner initDatabase(StorageService storageService) {
        return args -> {
            for (StorageType storageType : StorageType.values()) {
                StorageEntity storageEntity = StorageEntity.builder()
                        .storageType(storageType.name())
                        .bucket(storageType.getBucket())
                        .path(storageType.getPath())
                        .build();

                storageService.upsertStorage(storageEntity);
                logger.info("Ensured {} storage exists with bucket: {}", storageType.name(), storageType.getBucket());
            }
        };
    }
}