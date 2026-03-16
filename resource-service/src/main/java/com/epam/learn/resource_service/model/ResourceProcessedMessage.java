package com.epam.learn.resource_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Message sent by Resource Processor when a resource has been successfully processed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceProcessedMessage implements Serializable {

    private Long resourceId;
    private String status;
}
