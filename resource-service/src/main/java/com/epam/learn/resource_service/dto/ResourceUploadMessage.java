package com.epam.learn.resource_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUploadMessage implements Serializable {

    private Long resourceId;
    private String storageBucket;
    private String storageKey;
}
