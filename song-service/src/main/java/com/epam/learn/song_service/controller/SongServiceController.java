package com.epam.learn.song_service.controller;

import com.epam.learn.song_service.model.SongRequest;
import com.epam.learn.song_service.model.SongResponse;
import com.epam.learn.song_service.service.SongService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/songs")
@AllArgsConstructor

public class SongServiceController {

    private final SongService songService;

    @PostMapping()
    public ResponseEntity<SongResponse> uploadSong(@Valid @RequestBody SongRequest songRequest) {
        return ResponseEntity.ok(songService.uploadSong(songRequest));
    }


}
