package com.epam.learn.resource_service.controller;

import com.epam.learn.resource_service.service.ResourceService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/resources")
@AllArgsConstructor
@Validated
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping()
    public ResponseEntity<Map<String, Long>> uploadResource(@RequestBody byte[] file,
                                            @RequestHeader("Content-Type") String contentType) {

        return ResponseEntity.ok(resourceService.uploadResource(file, contentType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> downloadResource(
            @PathVariable
            @Min(value = 1, message = "Invalid value for ID '${validatedValue}'. Must be a positive integer")
            @NotNull
            Long id) {
        byte[] file = resourceService.downloadResource(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .contentLength(file.length)
                .body(file);
    }

    @DeleteMapping()
    public ResponseEntity<?> deleteResources(@RequestParam("id") String ids) {
        return resourceService.deleteResources(ids);
    }




}
