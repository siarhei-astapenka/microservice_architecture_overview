package com.epam.learn.resource_service.service;

import com.epam.learn.resource_service.client.MetadataClient;
import com.epam.learn.resource_service.exception.BadRequestException;
import com.epam.learn.resource_service.model.metadata.Metadata;
import com.epam.learn.resource_service.model.metadata.MetadataRequest;
import com.epam.learn.resource_service.model.metadata.MetadataResponse;
import com.epam.learn.resource_service.service.mapper.MetadataMapper;
import com.epam.learn.resource_service.service.parser.MetadataParser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class MetadataService {

    private final MetadataClient metadataClient;
    private final MetadataMapper metadataMapper;
    private final MetadataParser metadataParser;

    public MetadataResponse parceAndSaveMetadata(byte[]file, Long resourceId) {
        Metadata metadata =  metadataParser.getSongMetadataFromFile(file).orElseThrow(() -> new BadRequestException("No metadata"));

        MetadataRequest metadataRequest = metadataMapper.toSongMetadataRequest(metadata, resourceId);

        return metadataClient.postMetadata(metadataRequest);
    }

    public Map<String, List<Long>> deleteMetadata(String commaSeparatedIds) {
        return metadataClient.deleteMetadata(commaSeparatedIds);
    }
}
