package com.epam.learn.resource_service.cucumber;

import com.epam.learn.resource_service.client.SongServiceClient;
import com.epam.learn.resource_service.entity.ResourceEntity;
import com.epam.learn.resource_service.messaging.ResourceUploadProducer;
import com.epam.learn.resource_service.repository.ResourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cucumber step definitions for resource-service component tests.
 * Generic and concise implementation for CRUD operations.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ResourceServiceTestConfig.class)
public class ResourceServiceCucumberSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceRepository resourceRepository;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private ResourceUploadProducer resourceUploadProducer;

    @MockitoBean
    private SongServiceClient songServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    private ResultActions resultActions;
    private byte[] uploadedFileContent;
    private String contentType;
    private String idsParam;
    private Long resourceId;
    
    // Map to store logical IDs to actual database IDs
    private Map<String, Long> idMapping = new HashMap<>();

    @Before
    public void setUp() {
        resourceRepository.deleteAll();
        idMapping.clear();
        // Reset S3 mock
        reset(s3Client);
    }

    // ==================== BACKGROUND STEPS ====================

    @Given("the resource service is running")
    public void theResourceServiceIsRunning() {
        assertNotNull(mockMvc);
    }

    @Given("the S3 storage is available")
    public void theS3StorageIsAvailable() {
        when(s3Client.listBuckets(any(ListBucketsRequest.class)))
                .thenReturn(ListBucketsResponse.builder().buckets(
                        Bucket.builder().name("test-bucket").build()
                ).build());
    }

    @Given("the database is accessible")
    public void theDatabaseIsAccessible() {
        assertNotNull(resourceRepository);
    }

    // ==================== FILE PREPARATION STEPS ====================

    @Given("I have a {string} file")
    public void iHaveAFile(String fileType) throws IOException {
        switch (fileType) {
            case "valid-mp3":
                uploadedFileContent = loadFile("testdata/valid-sample-with-required-tags.mp3");
                contentType = "audio/mpeg";
                break;
            case "invalid-mp3":
                uploadedFileContent = loadFile("testdata/invalid-sample-with-missed-tags.mp3");
                contentType = "audio/mpeg";
                break;
            case "empty":
                uploadedFileContent = new byte[0];
                contentType = "audio/mpeg";
                break;
            case "oversized":
                uploadedFileContent = new byte[15 * 1024 * 1024];
                contentType = "audio/mpeg";
                break;
            case "valid-mp3-s3-down":
                uploadedFileContent = loadFile("testdata/valid-sample-with-required-tags.mp3");
                contentType = "audio/mpeg";
                when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                        .thenThrow(S3Exception.builder().message("S3 unavailable").build());
                break;
            default:
                uploadedFileContent = loadFile("testdata/valid-sample-with-required-tags.mp3");
                contentType = "audio/mpeg";
        }
    }

    private byte[] loadFile(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getInputStream().readAllBytes();
    }

    // ==================== GENERIC HTTP REQUEST STEPS ====================

    @When("I send POST request to {string} with file")
    public void iSendPostRequestWithFile(String endpoint) throws Exception {
        resultActions = mockMvc.perform(post(endpoint)
                .contentType(MediaType.parseMediaType(contentType))
                .content(uploadedFileContent));
    }

    @When("I send GET request to {string}")
    public void iSendGetRequest(String endpoint) throws Exception {
        // Resolve logical IDs in the endpoint
        String resolvedEndpoint = resolveIdsInEndpoint(endpoint);
        resultActions = mockMvc.perform(get(resolvedEndpoint));
    }
    
    private String resolveIdsInEndpoint(String endpoint) {
        // Extract ID from endpoint like "/resources/100" or "/resources/existing"
        String[] parts = endpoint.split("/");
        if (parts.length >= 3) {
            String lastPart = parts[parts.length - 1];
            if (idMapping.containsKey(lastPart)) {
                Long actualId = idMapping.get(lastPart);
                if (actualId > 0) {
                    parts[parts.length - 1] = actualId.toString();
                }
            }
        }
        return String.join("/", parts);
    }

    @When("I send DELETE request to {string} with query param {string}")
    public void iSendDeleteRequestWithQueryParam(String endpoint, String queryParam) throws Exception {
        // Parse the query param and resolve logical IDs to actual IDs
        String paramValue = queryParam.replace("id=", "");
        String resolvedIds = resolveIds(paramValue);
        resultActions = mockMvc.perform(delete(endpoint)
                .param("id", resolvedIds));
    }

    @When("I send DELETE request to {string} without id param")
    public void iSendDeleteRequestWithoutIdParam(String endpoint) throws Exception {
        resultActions = mockMvc.perform(delete(endpoint));
    }
    
    private String resolveIds(String idsParam) {
        StringBuilder result = new StringBuilder();
        String[] ids = idsParam.split(",");
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i].trim();
            if (idMapping.containsKey(id)) {
                Long actualId = idMapping.get(id);
                if (actualId > 0) {
                    result.append(actualId);
                } else {
                    // For non-existent IDs, use the original value (like 999)
                    result.append(id);
                }
            } else {
                // Try to parse as number - if it's a numeric ID not in mapping
                result.append(id);
            }
            if (i < ids.length - 1) {
                result.append(",");
            }
        }
        return result.toString();
    }

    // ==================== DATABASE PRECONDITION STEPS ====================

    @Given("resource with ID {string} exists in the system")
    public void resourceWithIdExistsInTheSystem(String logicalId) {
        ResourceEntity entity = ResourceEntity.builder()
                .storageBucket("test-bucket")
                .storageKey("test-song.mp3")
                .build();
        ResourceEntity saved = resourceRepository.save(entity);
        idMapping.put(logicalId, saved.getId());
        resourceId = saved.getId();
    }

    @Given("resources with IDs {string} exist in the system")
    public void resourcesWithIdsExistInTheSystem(String logicalIds) {
        String[] idArray = logicalIds.split(",");
        for (String logicalId : idArray) {
            ResourceEntity entity = ResourceEntity.builder()
                    .storageBucket("test-bucket")
                    .storageKey("test-song-" + logicalId.trim() + ".mp3")
                    .build();
            ResourceEntity saved = resourceRepository.save(entity);
            idMapping.put(logicalId.trim(), saved.getId());
        }
    }

    @Given("no resource exists")
    public void noResourceExists() {
        resourceRepository.deleteAll();
        idMapping.clear();
    }

    @Given("resource with ID {string} does not exist")
    public void resourceWithIdDoesNotExist(String logicalId) {
        // This is a no-op since the ID doesn't exist
        // Just record that this logical ID maps to a non-existent resource
        idMapping.put(logicalId, -1L);
    }

    @Given("the resource is stored in S3")
    public void theResourceIsStoredInS3() {
        byte[] mockData = "mock audio data".getBytes();
        GetObjectResponse response = GetObjectResponse.builder()
                .contentLength((long) mockData.length)
                .contentType("audio/mpeg")
                .build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response, mockData);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(responseBytes);
    }

    @Given("all resources are stored in S3")
    public void allResourcesAreStoredInS3() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());
    }

    @Given("the S3 storage fails to retrieve the file")
    public void theS3StorageFailsToRetrieveTheFile() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("File not found").build());
    }

    // ==================== ASSERTION STEPS ====================

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int statusCode) throws Exception {
        resultActions.andExpect(status().is(statusCode));
    }

    @Then("the response should contain {string}")
    public void theResponseShouldContain(String field) throws Exception {
        resultActions.andExpect(jsonPath("$." + field).exists());
    }

    @Then("the response content type should be {string}")
    public void theResponseContentTypeShouldBe(String contentType) throws Exception {
        resultActions.andExpect(content().contentTypeCompatibleWith(contentType));
    }
}
