package com.epam.learn.resource_processor.cucumber;

import com.epam.learn.resource_processor.client.ResourceServiceClient;
import com.epam.learn.resource_processor.client.SongServiceClient;
import com.epam.learn.resource_processor.dto.SongMetadataRequest;
import com.epam.learn.resource_processor.dto.SongMetadataResponse;
import com.epam.learn.resource_processor.service.ResourceProcessorService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cucumber step definitions for resource-processor component tests.
 * Tests the ResourceProcessorService by mocking external clients
 * (ResourceServiceClient and SongServiceClient).
 */
@CucumberContextConfiguration
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.rabbitmq.listener.direct.auto-startup=false",
                "spring.rabbitmq.dynamic=false",
                "management.health.rabbit.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@ActiveProfiles("test")
public class ResourceProcessorCucumberSteps {

    private static final Logger logger = LoggerFactory.getLogger(ResourceProcessorCucumberSteps.class);

    @Autowired
    private ResourceProcessorService resourceProcessorService;

    @MockitoBean
    private ResourceServiceClient resourceServiceClient;

    @MockitoBean
    private SongServiceClient songServiceClient;

    private Exception thrownException;
    private SongMetadataResponse lastSongMetadataResponse;
    private SongMetadataRequest capturedSongMetadataRequest;

    @Before
    public void setUp() {
        thrownException = null;
        lastSongMetadataResponse = null;
        capturedSongMetadataRequest = null;
        reset(resourceServiceClient, songServiceClient);

        // Default: song service returns a successful response
        when(songServiceClient.saveSongMetadata(any(SongMetadataRequest.class)))
                .thenAnswer(invocation -> {
                    capturedSongMetadataRequest = invocation.getArgument(0);
                    return SongMetadataResponse.builder()
                            .id(capturedSongMetadataRequest.getId())
                            .name(capturedSongMetadataRequest.getName())
                            .artist(capturedSongMetadataRequest.getArtist())
                            .album(capturedSongMetadataRequest.getAlbum())
                            .duration(capturedSongMetadataRequest.getDuration())
                            .year(capturedSongMetadataRequest.getYear())
                            .build();
                });
    }

    // ==================== BACKGROUND STEPS ====================

    @Given("the resource processor service is running")
    public void theResourceProcessorServiceIsRunning() {
        assertNotNull(resourceProcessorService);
        logger.info("Resource processor service is running");
    }

    @Given("the resource service client is available")
    public void theResourceServiceClientIsAvailable() {
        assertNotNull(resourceServiceClient);
        logger.info("Resource service client is available");
    }

    @Given("the song service client is available")
    public void theSongServiceClientIsAvailable() {
        assertNotNull(songServiceClient);
        logger.info("Song service client is available");
    }

    // ==================== PRECONDITION STEPS ====================

    @Given("a resource with ID {long} exists with a {string} file")
    public void aResourceWithIdExistsWithFile(Long resourceId, String fileType) throws IOException {
        byte[] fileData = loadFileByType(fileType);
        when(resourceServiceClient.getResourceData(resourceId)).thenReturn(fileData);
        logger.info("Configured resource service to return {} file for resourceId: {}", fileType, resourceId);
    }

    @Given("a resource with ID {long} exists with an {string} file")
    public void aResourceWithIdExistsWithAnFile(Long resourceId, String fileType) throws IOException {
        aResourceWithIdExistsWithFile(resourceId, fileType);
    }

    @Given("the resource service is unavailable for resource ID {long}")
    public void theResourceServiceIsUnavailableForResourceId(Long resourceId) {
        when(resourceServiceClient.getResourceData(resourceId))
                .thenThrow(new RuntimeException("Resource service unavailable"));
        logger.info("Configured resource service to throw exception for resourceId: {}", resourceId);
    }

    @Given("the resource service returns null data for resource ID {long}")
    public void theResourceServiceReturnsNullDataForResourceId(Long resourceId) {
        when(resourceServiceClient.getResourceData(resourceId)).thenReturn(null);
        logger.info("Configured resource service to return null for resourceId: {}", resourceId);
    }

    @Given("the song service is unavailable")
    public void theSongServiceIsUnavailable() {
        when(songServiceClient.saveSongMetadata(any(SongMetadataRequest.class)))
                .thenThrow(new RuntimeException("Song service unavailable"));
        logger.info("Configured song service to throw exception");
    }

    @Given("the song service returns a successful response with ID {long}")
    public void theSongServiceReturnsASuccessfulResponseWithId(Long responseId) {
        when(songServiceClient.saveSongMetadata(any(SongMetadataRequest.class)))
                .thenAnswer(invocation -> {
                    capturedSongMetadataRequest = invocation.getArgument(0);
                    lastSongMetadataResponse = SongMetadataResponse.builder()
                            .id(responseId)
                            .name(capturedSongMetadataRequest.getName())
                            .artist(capturedSongMetadataRequest.getArtist())
                            .album(capturedSongMetadataRequest.getAlbum())
                            .duration(capturedSongMetadataRequest.getDuration())
                            .year(capturedSongMetadataRequest.getYear())
                            .build();
                    return lastSongMetadataResponse;
                });
        logger.info("Configured song service to return response with ID: {}", responseId);
    }

    // ==================== ACTION STEPS ====================

    @When("the resource processor processes resource with ID {long}")
    public void theResourceProcessorProcessesResourceWithId(Long resourceId) {
        try {
            resourceProcessorService.processResource(resourceId);
            logger.info("Successfully processed resource with ID: {}", resourceId);
        } catch (Exception e) {
            thrownException = e;
            logger.info("Processing failed for resource ID: {} with exception: {}", resourceId, e.getMessage());
        }
    }

    @When("the resource processor attempts to process resource with ID {long}")
    public void theResourceProcessorAttemptsToProcessResourceWithId(Long resourceId) {
        theResourceProcessorProcessesResourceWithId(resourceId);
    }

    // ==================== ASSERTION STEPS ====================

    @Then("the processing should complete successfully")
    public void theProcessingShouldCompleteSuccessfully() {
        assertNull(thrownException,
                "Expected processing to succeed but got exception: " +
                        (thrownException != null ? thrownException.getMessage() : "none"));
        logger.info("Verified processing completed successfully");
    }

    @Then("the processing should fail with an exception")
    public void theProcessingShouldFailWithAnException() {
        assertNotNull(thrownException, "Expected processing to fail but it succeeded");
        logger.info("Verified processing failed with exception: {}", thrownException.getMessage());
    }

    @Then("the song metadata should be saved to the song service")
    public void theSongMetadataShouldBeSavedToTheSongService() {
        verify(songServiceClient, atLeastOnce()).saveSongMetadata(any(SongMetadataRequest.class));
        logger.info("Verified song metadata was saved to song service");
    }

    @Then("the song service should not be called")
    public void theSongServiceShouldNotBeCalled() {
        verify(songServiceClient, never()).saveSongMetadata(any(SongMetadataRequest.class));
        logger.info("Verified song service was not called");
    }

    @Then("the song service should receive metadata with field {string}")
    public void theSongServiceShouldReceiveMetadataWithField(String fieldName) {
        assertNotNull(capturedSongMetadataRequest,
                "No metadata request was captured - song service was not called");
        switch (fieldName) {
            case "name":
                assertNotNull(capturedSongMetadataRequest.getName(),
                        "Expected metadata field 'name' to be present");
                break;
            case "artist":
                assertNotNull(capturedSongMetadataRequest.getArtist(),
                        "Expected metadata field 'artist' to be present");
                break;
            case "album":
                assertNotNull(capturedSongMetadataRequest.getAlbum(),
                        "Expected metadata field 'album' to be present");
                break;
            case "duration":
                assertNotNull(capturedSongMetadataRequest.getDuration(),
                        "Expected metadata field 'duration' to be present");
                break;
            case "year":
                assertNotNull(capturedSongMetadataRequest.getYear(),
                        "Expected metadata field 'year' to be present");
                break;
            default:
                fail("Unknown metadata field: " + fieldName);
        }
        logger.info("Verified song service received metadata with field: {}", fieldName);
    }

    @Then("the song metadata response should contain ID {long}")
    public void theSongMetadataResponseShouldContainId(Long expectedId) {
        assertNotNull(lastSongMetadataResponse, "No song metadata response was captured");
        assertEquals(expectedId, lastSongMetadataResponse.getId(),
                "Expected song metadata response ID to be " + expectedId);
        logger.info("Verified song metadata response contains ID: {}", expectedId);
    }

    // ==================== HELPER METHODS ====================

    private byte[] loadFileByType(String fileType) throws IOException {
        return switch (fileType) {
            case "valid-mp3" -> loadFile("testdata/valid-sample-with-required-tags.mp3");
            case "invalid-mp3" -> loadFile("testdata/invalid-sample-with-missed-tags.mp3");
            case "empty" -> new byte[0];
            default -> loadFile("testdata/valid-sample-with-required-tags.mp3");
        };
    }

    private byte[] loadFile(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return resource.getInputStream().readAllBytes();
    }
}
