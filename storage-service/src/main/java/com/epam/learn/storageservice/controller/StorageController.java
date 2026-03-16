package com.epam.learn.storageservice.controller;

import com.epam.learn.storageservice.entity.StorageEntity;
import com.epam.learn.storageservice.mapper.StorageMapper;
import com.epam.learn.storageservice.model.Storage;
import com.epam.learn.storageservice.service.StorageService;
import com.epam.learn.storageservice.validation.constraints.ValidCsvLength;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/storages")
@Slf4j
public class StorageController {

    @Autowired
    private StorageService storageService;

    @Autowired
    private StorageMapper storageMapper;

    @PostMapping
    public ResponseEntity<Long> createStorage(@RequestBody Storage storage) {
        log.debug("Creating storage: {}", storage);
        StorageEntity storageEntity = storageMapper.toStorageEntity(storage);
        StorageEntity createdStorageEntity = storageService.createStorage(storageEntity);
        log.info("Created storage with id: {}", createdStorageEntity.getId());
        return ResponseEntity.ok(createdStorageEntity.getId());
    }

    @GetMapping
    public ResponseEntity<List<Storage>> getAllStorages() {
        log.debug("Getting all storages");
        List<StorageEntity> storageEntities = storageService.getAllStorages();
        List<Storage> storages = storageMapper.toStorageList(storageEntities);
        log.info("Retrieved {} storages", storages.size());
        return ResponseEntity.ok(storages);
    }

    @DeleteMapping
    public ResponseEntity<List<Long>> deleteStorages(
            @RequestParam
            @ValidCsvLength
            @Pattern(regexp = "^\\d++(,\\d++)*+$", message = "Id must be comma-separated numbers or single number")
            String id) {
        log.debug("Deleting storages with ids: {}", id);
        List<Long> ids = java.util.Arrays.stream(id.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        
        storageService.deleteStorages(ids);
        log.info("Deleted {} storages", ids.size());
        return ResponseEntity.ok(ids);
    }
}