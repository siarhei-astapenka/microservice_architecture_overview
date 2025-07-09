package com.epam.learn.resource_service.controller;

import com.epam.learn.resource_service.service.ResourceService;
import com.epam.learn.resource_service.validation.constraints.ValidCsvLength;
import com.epam.learn.resource_service.validation.constraints.ValidMP3;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
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
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping(consumes = "audio/mpeg")
    public ResponseEntity<Map<String, Long>> uploadResource(@RequestBody @ValidMP3 byte[] file) {
        return ResponseEntity.ok(resourceService.uploadResource(file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadResource(
            @PathVariable
            @Min(value = 1, message = "Invalid value for ID '${validatedValue}'. Must be a positive integer")
            @NotNull
            Long id
    ) {
        byte[] file = resourceService.downloadResource(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .contentLength(file.length)
                .body(file);
    }

    @DeleteMapping()
    public ResponseEntity<Map<String, List<Long>>> deleteResources(
            @RequestParam("id")
            @ValidCsvLength
            @Pattern(regexp = "^\\d+(,\\d+)*$", message = "'id' must be comma-separated numbers or single number")
            String ids
    ) {
        return ResponseEntity.ok(resourceService.deleteResources(ids));
    }
}
