# Positive Test Scenarios for Resource Processor
# BDD scenarios for successful resource processing operations

Feature: Resource Processing - Positive Scenarios
  As a resource processor service
  I want to process uploaded resources
  So that song metadata is extracted and saved to the song service

  Background:
    Given the resource processor service is running
    And the resource service client is available
    And the song service client is available

  @process
  Scenario Outline: Successfully process resource with valid MP3 file: <description>
    Given a resource with ID <resource_id> exists with a "<file_type>" file
    When the resource processor processes resource with ID <resource_id>
    Then the processing should complete successfully
    And the song metadata should be saved to the song service

    Examples:
      | description                          | resource_id | file_type |
      | Process valid MP3 with all tags      | 1           | valid-mp3 |

  @process
  Scenario: Successfully process resource and extract metadata fields
    Given a resource with ID 2 exists with a "valid-mp3" file
    When the resource processor processes resource with ID 2
    Then the processing should complete successfully
    And the song service should receive metadata with field "name"
    And the song service should receive metadata with field "artist"
    And the song service should receive metadata with field "album"

  @process
  Scenario: Successfully process resource and return song metadata response
    Given a resource with ID 3 exists with a "valid-mp3" file
    And the song service returns a successful response with ID 100
    When the resource processor processes resource with ID 3
    Then the processing should complete successfully
    And the song metadata response should contain ID 100
