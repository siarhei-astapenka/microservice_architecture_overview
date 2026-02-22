# Negative Test Scenarios for Resource Processor
# BDD scenarios for error cases in resource processing

Feature: Resource Processing - Negative Scenarios
  As a resource processor service
  I want to handle error cases gracefully
  So that failures are properly reported and resources are not silently lost

  Background:
    Given the resource processor service is running
    And the resource service client is available
    And the song service client is available

  @process
  Scenario Outline: Fail to process resource when resource service is unavailable: <description>
    Given the resource service is unavailable for resource ID <resource_id>
    When the resource processor attempts to process resource with ID <resource_id>
    Then the processing should fail with an exception
    And the song service should not be called

    Examples:
      | description                          | resource_id |
      | Resource service throws exception    | 10          |

  @process
  Scenario: Fail to process resource when file is empty
    Given a resource with ID 30 exists with an "empty" file
    When the resource processor attempts to process resource with ID 30
    Then the processing should fail with an exception
    And the song service should not be called

  @process
  Scenario: Fail to process resource when song service is unavailable
    Given a resource with ID 40 exists with a "valid-mp3" file
    And the song service is unavailable
    When the resource processor attempts to process resource with ID 40
    Then the processing should fail with an exception

  @process
  Scenario: Fail to process resource when resource data is null
    Given the resource service returns null data for resource ID 50
    When the resource processor attempts to process resource with ID 50
    Then the processing should fail with an exception
    And the song service should not be called
