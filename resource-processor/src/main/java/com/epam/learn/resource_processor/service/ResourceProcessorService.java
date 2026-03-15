package com.epam.learn.resource_processor.service;

import com.epam.learn.resource_processor.client.ResourceServiceClient;
import com.epam.learn.resource_processor.client.SongServiceClient;
import com.epam.learn.resource_processor.dto.SongMetadataRequest;
import com.epam.learn.resource_processor.dto.SongMetadataResponse;
import com.epam.learn.resource_processor.exception.ProcessingException;
import com.epam.learn.resource_processor.exception.BadRequestException;
import com.epam.learn.resource_processor.model.metadata.Metadata;
import com.epam.learn.resource_processor.service.mapper.MetadataMapper;
import com.epam.learn.resource_processor.service.parser.MetadataParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceProcessorService {

    private final ResourceServiceClient resourceServiceClient;
    private final SongServiceClient songServiceClient;
    private final MetadataParser metadataParser;
    private final MetadataMapper metadataMapper;

    public void processResource(Long resourceId) {
        log.info("Processing resource with ID: {}", resourceId);

        try {
            // Fetch binary data from resource-service
            byte[] resourceData = resourceServiceClient.getResourceData(resourceId);
            log.info("Fetched {} bytes for resourceId: {}", resourceData.length, resourceId);

            // Extract metadata using MetadataParser
            Metadata metadata =  metadataParser.getSongMetadataFromFile(resourceData).orElseThrow(() -> new BadRequestException("No metadata"));
            log.info("Extracted metadata for resourceId: {}: {}", resourceId, metadata);

            // Build SongMetadataRequest
            SongMetadataRequest songMetadata = metadataMapper.toSongMetadataRequest(metadata, resourceId);

            // Save metadata to song-service
            SongMetadataResponse response = songServiceClient.saveSongMetadata(songMetadata);
            log.info("Successfully processed resource {} and saved song metadata with ID: {}",
                    resourceId, response.getId());

            // Notify resource-service that processing is complete
            notifyProcessingComplete(resourceId);

        } catch (Exception e) {
            log.error("Error processing resource {}: {}", resourceId, e.getMessage());
            throw new ProcessingException("Failed to process resource: " + resourceId, e);
        }
    }

    /**
     * Notify the Resource Service that processing has completed successfully.
     * This triggers the move from STAGING to PERMANENT storage.
     */
    private void notifyProcessingComplete(Long resourceId) {
        try {
            resourceServiceClient.notifyProcessingComplete(resourceId);
            log.info("Sent processing complete notification for resourceId: {}", resourceId);
        } catch (Exception e) {
            log.error("Failed to notify processing complete for resourceId={}: {}", 
                    resourceId, e.getMessage());
            // Don't throw - processing was successful, notification is best-effort
        }
    }
}
