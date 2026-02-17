# Positive Test Scenarios for Resource Service
# Generic and concise BDD scenarios for resource upload, download, delete operations

Feature: Resource Management - Positive Scenarios
  As a client of the resource service
  I want to manage resource files
  So that I can upload, download, and delete audio resources

  Background:
    Given the resource service is running
    And the S3 storage is available
    And the database is accessible

  @upload
  Scenario Outline: Successfully upload resource: <description>
    Given I have a "<file_type>" file
    When I send POST request to "/resources" with file
    Then the response status code should be <status>
    And the response should contain "id"

    Examples:
      | description                  | file_type      | status |
      | Upload valid MP3 file        | valid-mp3      | 200    |

  @download
  Scenario Outline: Successfully download resource: <description>
    Given resource with ID "<resource_id>" exists in the system
    And the resource is stored in S3
    When I send GET request to "/resources/<resource_id>"
    Then the response status code should be <status>
    And the response content type should be "audio/mpeg"

    Examples:
      | description             | resource_id | status |
      | Download existing file | existing    | 200    |

  @delete
  Scenario Outline: Successfully delete resources: <description>
    Given resources with IDs "<resource_ids>" exist in the system
    And all resources are stored in S3
    When I send DELETE request to "/resources" with query param "id=<resource_ids>"
    Then the response status code should be <status>
    And the response should contain "ids"

    Examples:
      | description               | resource_ids   | status |
      | Delete multiple resources | first,second,third | 200 |
      | Delete single resource    | single         | 200    |
