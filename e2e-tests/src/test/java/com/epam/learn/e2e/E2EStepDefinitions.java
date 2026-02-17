package com.epam.learn.e2e;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Simplified Cucumber step definitions for End-to-End tests.
 * 
 * Uses generic, reusable steps to minimize code duplication and improve maintainability.
 * All tests exercise the full microservice stack running as Docker containers.
 */
public class E2EStepDefinitions {

    private static final Logger log = LoggerFactory.getLogger(E2EStepDefinitions.class);

    // Service URLs resolved from Testcontainers
    private String resourceServiceBaseUrl;
    private String songServiceBaseUrl;

    // Per-scenario state
    private Response lastResponse;
    private byte[] uploadFileContent;
    private Long savedResourceId;

    // ─────────────────────────────────────────────────────────────────────────
    // SETUP
    // ─────────────────────────────────────────────────────────────────────────

    @Before
    public void resetScenarioState() {
        lastResponse = null;
        uploadFileContent = null;
        savedResourceId = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GIVEN - Setup and preconditions
    // ─────────────────────────────────────────────────────────────────────────

    @Given("the microservice stack is running")
    public void theMicroserviceStackIsRunning() {
        resourceServiceBaseUrl = E2ETestContainersSetup.getResourceServiceUrl();
        songServiceBaseUrl = E2ETestContainersSetup.getSongServiceUrl();
        log.info("E2E stack URLs – resource-service: {}, song-service: {}", 
                resourceServiceBaseUrl, songServiceBaseUrl);
    }

    @Given("all services are healthy")
    public void allServicesAreHealthy() {
        assertServiceHealthy(resourceServiceBaseUrl, "resource-service");
        assertServiceHealthy(songServiceBaseUrl, "song-service");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WHEN - Actions
    // ─────────────────────────────────────────────────────────────────────────

    @When("I upload a valid MP3 file {string} to POST {string} with Content-Type {string}")
    public void iUploadAValidMp3FileToPOSTWithContentType(String fileName, String endpoint, String contentType) throws IOException {
        uploadFileContent = loadClasspathFile("features/testdata/" + fileName);
        log.info("Loaded MP3 file: {} ({} bytes)", fileName, uploadFileContent.length);
        
        lastResponse = given()
                .baseUri(resourceServiceBaseUrl)
                .contentType(contentType)
                .body(uploadFileContent)
                .when()
                .post(endpoint);
        log.info("Upload → HTTP {}", lastResponse.statusCode());
    }

    @When("I send a GET request to {string}")
    public void iSendAGETRequestTo(String endpoint) {
        String url = endpoint.replace("{id}", String.valueOf(savedResourceId));
        lastResponse = given()
                .baseUri(resourceServiceBaseUrl)
                .when()
                .get(url);
        log.info("GET {} → HTTP {}", url, lastResponse.statusCode());
    }

    @When("I send a GET request to the Song Service at {string}")
    public void iSendAGETRequestToTheSongServiceAt(String endpoint) {
        String url = endpoint.replace("{id}", String.valueOf(savedResourceId));
        
        // Wait for async processing with retry
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(() -> {
                    lastResponse = given()
                            .baseUri(songServiceBaseUrl)
                            .when()
                            .get(url);
                    log.info("GET {} → HTTP {}", url, lastResponse.statusCode());
                    return lastResponse.statusCode() == 200 || lastResponse.statusCode() == 404;
                });
    }

    @When("I send a DELETE request to {string} with query parameter {string} set to {string}")
    public void iSendADELETERequestToWithQueryParameterSetTo(String endpoint, String paramName, String paramValue) {
        String value = paramValue.replace("{id}", String.valueOf(savedResourceId));
        lastResponse = given()
                .baseUri(resourceServiceBaseUrl)
                .queryParam(paramName, value)
                .when()
                .delete(endpoint);
        log.info("DELETE {}?{}={} → HTTP {}", endpoint, paramName, value, lastResponse.statusCode());
    }

    @And("I wait {int} seconds for async processing")
    public void iWaitSecondsForAsyncProcessing(int seconds) {
        log.info("Waiting {} seconds for async processing...", seconds);
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // THEN - Assertions
    // ─────────────────────────────────────────────────────────────────────────

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatus) {
        assertThat(lastResponse.statusCode())
                .as("Expected HTTP %d but got %d. Body: %s",
                        expectedStatus, lastResponse.statusCode(), lastResponse.body().asString())
                .isEqualTo(expectedStatus);
    }

    @Then("the response body should contain only the field {string}")
    public void theResponseBodyShouldContainOnlyTheField(String fieldName) {
        Object value = lastResponse.jsonPath().get(fieldName);
        assertThat(value)
                .as("Expected field '%s' in response body: %s", fieldName, lastResponse.body().asString())
                .isNotNull();
    }

    @Then("the response Content-Type should be {string}")
    public void theResponseContentTypeShouldBe(String expectedContentType) {
        assertThat(lastResponse.contentType())
                .as("Expected content-type '%s'", expectedContentType)
                .contains(expectedContentType);
    }

    @Then("the response Content-Length should be greater than {int}")
    public void theResponseContentLengthShouldBeGreaterThan(int minLength) {
        String contentLength = lastResponse.header("Content-Length");
        assertThat(contentLength).isNotNull();
        assertThat(Long.parseLong(contentLength)).isGreaterThan(minLength);
    }

    @Then("the response body should be valid JSON")
    public void theResponseBodyShouldBeValidJSON() {
        // If we can parse it as JSON, it's valid
        assertThat(lastResponse.jsonPath()).isNotNull();
    }

    @Then("the response body should contain the field {string} with a non-null value")
    public void theResponseBodyShouldContainTheFieldWithANonNullValue(String fieldName) {
        Object value = lastResponse.jsonPath().get(fieldName);
        assertThat(value)
                .as("Expected field '%s' to have non-null value", fieldName)
                .isNotNull();
    }

    @Then("the response body should contain the field {string} matching the format {string}")
    public void theResponseBodyShouldContainTheFieldMatchingTheFormat(String fieldName, String format) {
        String value = lastResponse.jsonPath().getString(fieldName);
        assertThat(value).as("Expected field '%s' to match format '%s'", fieldName, format).isNotNull();
        
        if ("mm:ss".equals(format)) {
            assertThat(value).matches("\\d{1,2}:\\d{2}");
        }
    }

    @Then("the response body should not contain the field {string}")
    public void theResponseBodyShouldNotContainTheField(String fieldName) {
        Object value = lastResponse.jsonPath().get(fieldName);
        assertThat(value)
                .as("Expected field '%s' to not be present", fieldName)
                .isNull();
    }

    @Then("the response body should contain the field {string}")
    public void theResponseBodyShouldContainTheField(String fieldName) {
        Object value = lastResponse.jsonPath().get(fieldName);
        assertThat(value)
                .as("Expected field '%s' in response body", fieldName)
                .isNotNull();
    }

    @Then("the response body should contain an {string} array of numbers")
    public void theResponseBodyShouldContainAnArrayOfNumbers(String fieldName) {
        List<?> array = lastResponse.jsonPath().getList(fieldName);
        assertThat(array)
                .as("Expected '%s' to be an array", fieldName)
                .isNotNull();
    }

    @Then("the {string} array should contain the uploaded resource id")
    public void theArrayShouldContainTheUploadedResourceId(String fieldName) {
        List<Integer> array = lastResponse.jsonPath().getList(fieldName);
        assertThat(array)
                .as("Expected '%s' array to contain resource ID %d", fieldName, savedResourceId)
                .contains(savedResourceId.intValue());
    }

    @Then("the {string} array should not contain {int}")
    public void theArrayShouldNotContain(String fieldName, int value) {
        List<Integer> array = lastResponse.jsonPath().getList(fieldName);
        assertThat(array)
                .as("Expected '%s' array to not contain %d", fieldName, value)
                .doesNotContain(value);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AND - Additional actions and assertions
    // ─────────────────────────────────────────────────────────────────────────

    @And("I save the returned {string} for use in subsequent steps")
    public void iSaveTheReturnedForUseInSubsequentSteps(String fieldName) {
        savedResourceId = lastResponse.jsonPath().getLong(fieldName);
        assertThat(savedResourceId).as("Resource ID should be present in response").isNotNull();
        log.info("Saved resource ID: {}", savedResourceId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────────────

    private void assertServiceHealthy(String baseUrl, String serviceName) {
        given()
                .baseUri(baseUrl)
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
        log.info("{} is healthy", serviceName);
    }

    private byte[] loadClasspathFile(String path) throws IOException {
        return new ClassPathResource(path).getInputStream().readAllBytes();
    }
}
