package com.epam.learn.resource_service.service;

import com.epam.learn.resource_service.entity.ResourceEntity;
import com.epam.learn.resource_service.exception.NotFoundException;
import com.epam.learn.resource_service.repository.ResourceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public Map<String, Long> uploadResource(byte[] file) {
        ResourceEntity entity = ResourceEntity.builder()
                .fileData(file)
                .build();

        entity = resourceRepository.save(entity);

        Map<String, Long> response = new HashMap<>();
        response.put("id", entity.getId());

        return response;
    }

    public byte[] downloadResource(Long id) {
        byte[] fileData;

        try {
            fileData = resourceRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("Resource with ID=%s not found", id))).getFileData();
        } catch (Exception e) {
            throw new NotFoundException(String.format("Resource with ID=%s not found", id));
        }

        if (fileData == null) {
            throw new NotFoundException(String.format("Resource with ID=%s not found", id));
        }
        return fileData;
    }

    public Map<String, List<Long>> deleteResources(String ids) {
        Map<String, List<Long>> response = new HashMap<>();

        List<Long> idsToDelete = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Long> existingIds = resourceRepository.findExistingIds(idsToDelete);

        resourceRepository.deleteAllById(existingIds);

        response.put("ids", existingIds);

        return response;
    }
}
