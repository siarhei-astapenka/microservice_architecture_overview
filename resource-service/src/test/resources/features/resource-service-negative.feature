# Negative Test Scenarios for Resource Service
# Generic and concise BDD scenarios for error cases

Feature: Resource Management - Negative Scenarios
  As a client of the resource service
  I want to handle error cases gracefully
  So that I receive appropriate error messages when operations fail

  Background:
    Given the resource service is running
    And the S3 storage is available
    And the database is accessible

  @upload
  Scenario Outline: Fail to upload resource: <description>
    Given I have a "<file_type>" file
    When I send POST request to "/resources" with file
    Then the response status code should be <status>
    And the response should contain "errorMessage"

    Examples:
      | description                     | file_type        | status |
      | Upload empty file               | empty            | 400    |
      | Upload file too large           | oversized        | 400    |
      | Upload when S3 unavailable      | valid-mp3-s3-down | 503    |

  @download
  Scenario: Fail to download non-existent resource
    Given no resource exists
    When I send GET request to "/resources/99999"
    Then the response status code should be 404
    And the response should contain "errorMessage"

  @download
  Scenario: Fail to download resource with invalid ID
    When I send GET request to "/resources/-1"
    Then the response status code should be 400
    And the response should contain "errorMessage"

  @download
  Scenario: Fail to download resource when S3 fails
    Given resource with ID "s3fail" exists in the system
    And the S3 storage fails to retrieve the file
    When I send GET request to "/resources/s3fail"
    Then the response status code should be 503
    And the response should contain "errorMessage"

  @delete
  Scenario: Fail to delete resource with invalid ID format
    When I send DELETE request to "/resources" with query param "id=abc"
    Then the response status code should be 400
    And the response should contain "errorMessage"

  @delete
  Scenario: Fail to delete resource without IDs
    When I send DELETE request to "/resources" without id param
    Then the response status code should be 400
    And the response should contain "errorMessage"

  @delete
  Scenario Outline: Delete with mixed results: <description>
    Given resources with IDs "<resource_ids>" exist in the system
    And all resources are stored in S3
    When I send DELETE request to "/resources" with query param "id=<resource_ids>,999"
    Then the response status code should be <status>
    And the response should contain "ids"

    Examples:
      | description                        | resource_ids | status |
      | Delete mixed existing/non-existent | first,second | 200    |
