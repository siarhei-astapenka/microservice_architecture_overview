package com.epam.learn.storageservice.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StorageType {
    STAGING("staging-bucket", "/files"),
    PERMANENT("permanent-bucket", "/files");

    private final String bucket;
    private final String path;
}
