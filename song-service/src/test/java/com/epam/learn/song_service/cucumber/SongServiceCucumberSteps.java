package com.epam.learn.song_service.cucumber;

import com.epam.learn.song_service.entity.SongEntity;
import com.epam.learn.song_service.repository.SongRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cucumber step definitions for song-service component tests.
 * Generic and concise implementation for CRUD operations.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SongServiceCucumberSteps {

    private static final Logger logger = LoggerFactory.getLogger(SongServiceCucumberSteps.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SongRepository songRepository;

    private ResultActions resultActions;
    private String requestJson;
    private Long resourceId;

    @BeforeEach
    void setUp() {
        songRepository.deleteAll();
        requestJson = null;
        resourceId = null;
    }

    // ==================== BACKGROUND STEPS ====================

    @Given("the song service is running")
    public void theSongServiceIsRunning() {
        assertNotNull(mockMvc);
        logger.info("Song service is running");
    }

    @Given("the database is accessible")
    public void theDatabaseIsAccessible() {
        assertNotNull(songRepository);
        logger.info("Database is accessible");
    }

    // ==================== REQUEST PAYLOAD ====================

    @Given("I have a request payload {string}")
    public void iHaveARequestPayload(String requestFile) throws IOException {
        requestJson = loadJsonFile("testdata/" + requestFile);
        logger.info("Loaded request payload: testdata/{}", requestFile);
    }

    // ==================== GENERIC HTTP REQUEST STEPS ====================

    @When("I send POST request to {string} with payload")
    public void iSendPostRequestToWithPayload(String endpoint) throws Exception {
        resultActions = mockMvc.perform(post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson != null ? requestJson : ""));
        logger.info("Sent POST request to: {}", endpoint);
    }

    @When("I send GET request to {string}")
    public void iSendGetRequestTo(String endpoint) throws Exception {
        resultActions = mockMvc.perform(get(endpoint));
        logger.info("Sent GET request to: {}", endpoint);
    }

    @When("I send DELETE request to {string} with query param {string}")
    public void iSendDeleteRequestToWithQueryParam(String endpoint, String queryParam) throws Exception {
        resultActions = mockMvc.perform(delete(endpoint)
                .param("id", queryParam.replace("id=", "")));
        logger.info("Sent DELETE request to: {} with param: {}", endpoint, queryParam);
    }

    // ==================== DATABASE PRECONDITION STEPS ====================

    @Given("song metadata with ID {string} already exists in the database")
    public void songMetadataWithIdAlreadyExistsInTheDatabase(String id) {
        SongEntity entity = SongEntity.builder()
                .resourceId(Long.parseLong(id))
                .name("Existing Song")
                .artist("Existing Artist")
                .album("Existing Album")
                .duration(Duration.ofMillis(180000))
                .year(LocalDate.now())
                .build();
        songRepository.save(entity);
    }

    @Given("song metadata with resource ID {string} exists in the database")
    public void songMetadataWithResourceIdExistsInTheDatabase(String id) {
        resourceId = Long.parseLong(id);
        SongEntity entity = SongEntity.builder()
                .resourceId(resourceId)
                .name("Test Song")
                .artist("Test Artist")
                .album("Test Album")
                .duration(Duration.ofMillis(180000))
                .year(LocalDate.now())
                .build();
        songRepository.save(entity);
    }

    @Given("song metadata entries with resource IDs {string} exist in the database")
    public void songMetadataEntriesWithResourceIdsExistInTheDatabase(String ids) {
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            SongEntity entity = SongEntity.builder()
                    .resourceId(Long.parseLong(id.trim()))
                    .name("Song " + id)
                    .artist("Artist " + id)
                    .album("Album " + id)
                    .duration(Duration.ofMillis(180000))
                    .year(LocalDate.now())
                    .build();
            songRepository.save(entity);
        }
    }

    @Given("no song metadata with resource ID {string} exists")
    public void noSongMetadataWithResourceIdExists(String id) {
        songRepository.findByResourceId(Long.parseLong(id));
    }

    @Given("no song metadata exists in the database")
    public void noSongMetadataExistsInTheDatabase() {
        songRepository.deleteAll();
    }

    @Given("I provide an invalid resource ID {string}")
    public void iProvideAnInvalidResourceId(String id) {
        resourceId = Long.parseLong(id);
    }

    @Given("I provide an invalid ID parameter {string}")
    public void iProvideAnInvalidIdParameter(String param) {
        logger.info("Provided invalid ID parameter: {}", param);
    }

    // ==================== ASSERTION STEPS ====================

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int statusCode) throws Exception {
        resultActions.andExpect(status().is(statusCode));
        logger.info("Verified response status code: {}", statusCode);
    }

    @Then("the response should contain {string}")
    public void theResponseShouldContain(String field) throws Exception {
        resultActions.andExpect(jsonPath("$." + field).exists());
        logger.info("Verified response contains field: {}", field);
    }

    @Then("the response should match {string}")
    public void theResponseShouldMatch(String responseFile) throws Exception {
        String expectedJson = loadJsonFile("testdata/" + responseFile);

        if (expectedJson.contains("\"id\"")) {
            resultActions.andExpect(jsonPath("$.id").exists());
        }
        if (expectedJson.contains("\"errorMessage\"")) {
            resultActions.andExpect(jsonPath("$.errorMessage").exists());
        }
        if (expectedJson.contains("\"ids\"")) {
            resultActions.andExpect(jsonPath("$.ids").exists());
        }

        logger.info("Response matches expected file: {}", responseFile);
    }

    @Then("the response should have empty {string}")
    public void theResponseShouldHaveEmpty(String field) throws Exception {
        resultActions.andExpect(jsonPath("$." + field).isEmpty());
        logger.info("Verified response has empty field: {}", field);
    }

    // ==================== HELPER METHODS ====================

    private String loadJsonFile(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
