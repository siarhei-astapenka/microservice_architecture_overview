package com.epam.learn.resource_service.enumeration;

/**
 * Enum representing the processing state of a resource file.
 * STAGING - File is in processing/pending state.
 * PERMANENT - File has been successfully processed and stored permanently.
 */
public enum ResourceState {
    STAGING,
    PERMANENT
}
