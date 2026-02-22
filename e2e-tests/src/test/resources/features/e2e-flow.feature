# End-to-End Test Scenarios: Complete MP3 Processing Flow
# Covers: Upload → Processing → Download → Delete
# All services run as real Docker containers via Testcontainers

Feature: E2E - Complete MP3 File Processing Flow
  As a client of the microservice system
  I want to upload, process, download, and delete MP3 files
  So that the system handles the complete lifecycle correctly

  Background:
    Given the microservice stack is running
    And all services are healthy

  @e2e @happy-path
  Scenario: Full lifecycle of an MP3 resource and its song metadata
    # Step 1: Upload a valid MP3 resource
    When I upload a valid MP3 file "valid-sample-with-required-tags.mp3" to POST "/resources" with Content-Type "audio/mpeg"
    Then the response status code should be 200
    And the response body should contain only the field "id"
    And I save the returned "id" for use in subsequent steps

    # Step 2: Verify the uploaded resource can be retrieved
    When I send a GET request to "/resources/{id}"
    Then the response status code should be 200
    And the response Content-Type should be "audio/mpeg"
    And the response Content-Length should be greater than 0

    # Step 3: Verify the song metadata was automatically created
    When I send a GET request to the Song Service at "/songs/{id}"
    Then the response status code should be 200
    And the response body should be valid JSON
    And the response body should contain the field "id" with a non-null value
    And the response body should contain the field "name" with a non-null value
    And the response body should contain the field "artist" with a non-null value
    And the response body should contain the field "album" with a non-null value
    And the response body should contain the field "duration" matching the format "mm:ss"
    And the response body should contain the field "year" with a non-null value
    And the response body should not contain the field "resourceId"
    And the response body should not contain the field "length"

    # Step 4: Delete the resource (and its metadata) along with non-existent IDs 101 and 102
    When I send a DELETE request to "/resources" with query parameter "id" set to "{id},101,102"
    Then the response status code should be 200
    And the response body should be valid JSON
    And the response body should contain an "ids" array of numbers
    And the "ids" array should contain the uploaded resource id
    And the "ids" array should not contain 101
    And the "ids" array should not contain 102

    # Step 5: Confirm the resource is no longer accessible
    When I send a GET request to "/resources/{id}"
    Then the response status code should be 404
    And the response body should be valid JSON
    And the response body should contain the field "errorMessage"
    And the response body should contain the field "errorCode"

    # Step 6: Confirm the song metadata is also no longer accessible
    When I send a GET request to the Song Service at "/songs/{id}"
    Then the response status code should be 404
    And the response body should be valid JSON
    And the response body should contain the field "errorMessage"
    And the response body should contain the field "errorCode"
