package com.epam.learn.song_service.controller;

import com.epam.learn.song_service.model.SongRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/songs")
@AllArgsConstructor
@Validated
public class SongServiceController {

    @PostMapping()
    public ResponseEntity<SongRequest> uploadSong(@Valid @RequestBody SongRequest songRequest) {
        return ResponseEntity.ok(songRequest);
    }

}
