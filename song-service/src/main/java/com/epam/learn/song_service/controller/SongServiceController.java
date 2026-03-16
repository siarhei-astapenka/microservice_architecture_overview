package com.epam.learn.song_service.controller;

import com.epam.learn.song_service.model.SongMetadataRequest;
import com.epam.learn.song_service.model.SongMetadataResponse;
import com.epam.learn.song_service.service.SongService;
import com.epam.learn.song_service.validation.constraints.ValidCsvLength;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/songs")
@AllArgsConstructor
@Validated
@Slf4j
public class SongServiceController {

    private final SongService songService;

    @PostMapping()
    public ResponseEntity<SongMetadataResponse> postSongMetadata(@Valid @RequestBody SongMetadataRequest songMetadataRequest) {
        log.info("POST /songs - Saving song metadata for resourceId: {}", songMetadataRequest.getId());
        log.debug("Song metadata request: {}", songMetadataRequest);
        
        SongMetadataResponse response = songService.saveSongMetadata(songMetadataRequest);
        
        log.info("POST /songs - Successfully saved song metadata with id: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongMetadataResponse> getSongMetadata(
            @PathVariable
            @Min(value = 1, message = "Invalid value for ID '${validatedValue}'. Must be a positive integer")
            @NotNull
            Long id
    ) {
        log.info("GET /songs/{} - Retrieving song metadata by resourceId", id);
        
        SongMetadataResponse response = songService.getSongMetadataByResourceId(id);
        
        log.debug("GET /songs/{} - Retrieved song metadata: {}", id, response);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping()
    public ResponseEntity<Map<String, List<Long>>> deleteSongMetadata(
            @RequestParam("id")
            @ValidCsvLength
            @Pattern(regexp = "^\\d++(,\\d++)*+$", message = "Id must be comma-separated numbers or single number")
            String ids
    ) {
        log.info("DELETE /songs?id={} - Deleting song metadata", ids);
        
        Map<String, List<Long>> response = songService.deleteSongMetadata(ids);
        
        log.info("DELETE /songs - Successfully deleted {} song(s) with resourceIds: {}", 
                response.get("ids").size(), response.get("ids"));
        return ResponseEntity.ok(response);
    }
}
