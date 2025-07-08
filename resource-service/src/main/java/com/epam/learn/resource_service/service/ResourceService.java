package com.epam.learn.resource_service.service;

import com.epam.learn.resource_service.entity.ResourceEntity;
import com.epam.learn.resource_service.exception.BadRequestException;
import com.epam.learn.resource_service.exception.NotFoundException;
import com.epam.learn.resource_service.repository.ResourceRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;



@Service
@AllArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional
    public Map<String, Long> uploadResource(byte[] file, String contentType) {
        validateUploadRequest(file, contentType);

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

    public ResponseEntity<?> deleteResources(String ids) {
        Map<String, List<Long>> response = new HashMap<>();

        try {
            List<Long> idsToDelete = Arrays.stream(ids.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            List<Long> existingIds = resourceRepository.findExistingIds(idsToDelete);

            resourceRepository.deleteAllById(existingIds);

            response.put("ids", existingIds);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number in string");
        }

        return ResponseEntity.ok(response);
    }

    private void validateUploadRequest(byte[] file, String contentType) {
        if (!"audio/mpeg".equals(contentType)) {
            throw new BadRequestException(String.format("Invalid file format: %s. Only MP3 files are allowed", contentType));
        }

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(file);
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();

            Parser parser = new AutoDetectParser();

            parser.parse(inputStream, handler, metadata, new ParseContext());

            String mimeType = metadata.get(Metadata.CONTENT_TYPE);

            if (!"audio/mpeg".equals(mimeType)) {
                throw new BadRequestException("Invalid file format. Only MP3 files are allowed");
            }
        } catch (IOException | SAXException | TikaException e) {
            throw new BadRequestException("Invalid file format. Only MP3 files are allowed");
        }
    }


}
