package com.epam.learn.song_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class SongServiceApplication {

    public static void main(String[] args) {
        log.info("Starting Song Service...");
        SpringApplication.run(SongServiceApplication.class, args);
        log.info("Song Service started successfully");
    }
}
