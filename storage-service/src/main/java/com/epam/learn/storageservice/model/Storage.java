package com.epam.learn.storageservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Storage {
    private Long id;
    private String storageType;
    private String bucket;
    private String path;
}