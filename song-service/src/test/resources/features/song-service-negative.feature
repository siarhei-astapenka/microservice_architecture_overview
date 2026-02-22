# Negative Test Scenarios for Song Service
# Generic and concise BDD scenarios for error cases

Feature: Song Metadata Management - Negative Scenarios
  As a client of the song service
  I want to handle error cases gracefully
  So that I receive appropriate error messages when operations fail

  Background:
    Given the song service is running
    And the database is accessible

  @create
  Scenario Outline: Fail to create song metadata: <description>
    Given I have a request payload "<request_file>"
    When I send POST request to "/songs" with payload
    Then the response status code should be <status>
    And the response should contain "errorMessage"
    And the response should match "<response_file>"

    Examples:
      | description                       | request_file                                      | response_file                                                | status |
      | Create song with missing title    | requests/negative/create_missing_title.json       | responses/negative/create_missing_title_response.json       | 400    |
      | Create song with missing artist   | requests/negative/create_missing_artist.json      | responses/negative/create_missing_artist_response.json      | 400    |
      | Create song with invalid duration | requests/negative/create_invalid_duration.json   | responses/negative/create_invalid_duration_response.json    | 400    |

  @create
  Scenario Outline: Fail to create duplicate song metadata: <description>
    Given song metadata with ID "105" already exists in the database
    And I have a request payload "<request_file>"
    When I send POST request to "/songs" with payload
    Then the response status code should be <status>
    And the response should contain "errorMessage"
    And the response should match "<response_file>"

    Examples:
      | description                | request_file                         | response_file                                 | status |
      | Create duplicate song      | requests/negative/create_duplicate.json | responses/negative/create_duplicate_response.json | 409    |

  @retrieve
  Scenario Outline: Fail to retrieve song metadata: <description>
    Given no song metadata with resource ID "<resource_id>" exists
    When I send GET request to "/songs/<resource_id>"
    Then the response status code should be <status>
    And the response should contain "errorMessage"
    And the response should match "<response_file>"

    Examples:
      | description                    | resource_id | response_file                                   | status |
      | Get non-existent song         | 99999       | responses/negative/get_not_found_response.json | 404    |
      | Get song with invalid ID      | -1          | responses/negative/get_invalid_id_response.json | 400    |

  @delete
  Scenario Outline: Fail to delete song metadata: <description>
    Given I provide an invalid ID parameter "<param>"
    When I send DELETE request to "/songs" with query param "id=<param>"
    Then the response status code should be <status>
    And the response should contain "errorMessage"
    And the response should match "<response_file>"

    Examples:
      | description                  | param | response_file                                           | status |
      | Delete with invalid ID      | abc   | responses/negative/delete_invalid_ids_response.json    | 400    |

  @delete
  Scenario Outline: Delete returns empty list: <description>
    Given no song metadata exists in the database
    When I send DELETE request to "/songs" with query param "id=1000,1001"
    Then the response status code should be <status>
    And the response should contain "ids"
    And the response should have empty "ids"

    Examples:
      | description                               | status |
      | Delete non-existent IDs returns empty list | 200    |
