package com.epam.learn.song_service.controller;

import com.epam.learn.song_service.model.SongRequest;
import com.epam.learn.song_service.model.SongResponse;
import com.epam.learn.song_service.service.SongService;
import com.epam.learn.song_service.validation.constraints.ValidCsvLength;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
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
public class SongServiceController {

    private final SongService songService;

    @PostMapping()
    public ResponseEntity<SongResponse> postSongMetadata(@Valid @RequestBody SongRequest songRequest) {
        return ResponseEntity.ok(songService.saveSongMetadata(songRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongResponse> getSongMetadata(
            @PathVariable
            @Min(value = 1, message = "Invalid value for ID '${validatedValue}'. Must be a positive integer")
            @NotNull
            Long id
    ) {
        return ResponseEntity.ok(songService.getSongMetadataByResourceId(id));
    }

    @DeleteMapping()
    public ResponseEntity<Map<String, List<Long>>> deleteSongMetadata(
            @RequestParam("id")
            @ValidCsvLength
            @Pattern(regexp = "^\\d+(,\\d+)*$", message = "Id must be comma-separated numbers or single number")
            String ids
    ) {
        return ResponseEntity.ok(songService.deleteSongMetadata(ids));
    }
}
