package com.epam.learn.resource_service.controller;

import com.epam.learn.resource_service.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling processing completion notifications from Resource Processor.
 */
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@Slf4j
public class ResourceProcessingController {

    private final ResourceService resourceService;

    /**
     * Endpoint called by Resource Processor when a resource has been successfully processed.
     * This triggers the move from STAGING to PERMANENT storage.
     */
    @PostMapping("/{id}/processed")
    public ResponseEntity<Void> markAsProcessed(@PathVariable Long id) {
        log.info("Received processing complete notification for resourceId: {}", id);
        resourceService.handleProcessingComplete(id);
        return ResponseEntity.ok().build();
    }
}
