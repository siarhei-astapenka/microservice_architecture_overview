package com.epam.learn.resource_service.controller;

import com.epam.learn.resource_service.service.ResourceService;
import com.epam.learn.resource_service.validation.constraints.ValidCsvLength;
import com.epam.learn.resource_service.validation.constraints.ValidMP3;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resources")
@AllArgsConstructor
@Validated
@Slf4j
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping(consumes = "audio/mpeg")
    public ResponseEntity<Map<String, Long>> uploadResource(@RequestBody @ValidMP3 byte[] file) {
        log.info("HTTP POST /resources called, incoming file size={} bytes", file != null ? file.length : 0);
        Map<String, Long> result = resourceService.uploadResource(file);
        log.info("HTTP POST /resources completed, created id={}", result != null ? result.get("id") : null);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadResource(
            @PathVariable
            @Min(value = 1, message = "Invalid value for ID '${validatedValue}'. Must be a positive integer")
            @NotNull
            Long id
    ) {
        log.info("HTTP GET /resources/{} called", id);
        byte[] file = resourceService.downloadResource(id);
        log.info("HTTP GET /resources/{} completed, returning {} bytes", id, file != null ? file.length : 0);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .contentLength(file.length)
                .body(file);
    }

    @DeleteMapping()
    public ResponseEntity<Map<String, List<Long>>> deleteResources(
            @RequestParam("id")
            @ValidCsvLength
            @Pattern(regexp = "^\\d++(,\\d++)*+$", message = "'id' must be comma-separated numbers or single number")
            String ids
    ) {
        log.info("HTTP DELETE /resources called for ids='{}'", ids);
        Map<String, List<Long>> result = resourceService.deleteResources(ids);
        log.info("HTTP DELETE /resources completed, deleted ids={}", result != null ? result.get("ids") : null);
        return ResponseEntity.ok(result);
    }
}
