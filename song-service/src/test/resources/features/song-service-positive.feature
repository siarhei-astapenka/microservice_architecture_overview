# Positive Test Scenarios for Song Service
# Generic and concise BDD scenarios for song metadata operations

Feature: Song Metadata Management - CRUD Operations
  As a client of the song service
  I want to perform CRUD operations on song metadata
  So that I can create, retrieve, update and delete song information

  Background:
    Given the song service is running
    And the database is accessible

  @create
  Scenario Outline: Successfully create song metadata: <description>
    Given I have a request payload "<request_file>"
    When I send POST request to "/songs" with payload
    Then the response status code should be <status>
    And the response should contain "id"
    And the response should match "<response_file>"

    Examples:
      | description                            | request_file                                     | response_file                                                | status |
      | Create song with all fields            | requests/positive/create_valid_all_fields.json   | responses/positive/create_valid_all_fields_response.json     | 200    |
      | Create song with required fields only  | requests/positive/create_valid_required_fields.json | responses/positive/create_valid_required_fields_response.json | 200    |

  @retrieve
  Scenario Outline: Successfully retrieve song metadata: <description>
    Given song metadata with resource ID "<resource_id>" exists in the database
    When I send GET request to "/songs/<resource_id>"
    Then the response status code should be <status>
    And the response should match "<response_file>"

    Examples:
      | description                    | resource_id | response_file                                       | status |
      | Retrieve existing song        | 200         | responses/positive/get_by_resource_id_200_response.json | 200    |

  @delete
  Scenario Outline: Successfully delete song metadata: <description>
    Given song metadata entries with resource IDs "<resource_ids>" exist in the database
    When I send DELETE request to "/songs" with query param "id=<resource_ids>"
    Then the response status code should be <status>
    And the response should contain "ids"
    And the response should match "<response_file>"

    Examples:
      | description                    | resource_ids | response_file                                         | status |
      | Delete multiple song entries   | 300,301,302 | responses/positive/delete_multiple_ids_response.json | 200    |

  @delete
  Scenario Outline: Successfully delete single song metadata: <description>
    Given song metadata with resource ID "<resource_id>" exists in the database
    When I send DELETE request to "/songs" with query param "id=<resource_id>"
    Then the response status code should be <status>
    And the response should contain "ids"

    Examples:
      | description                | resource_id | status |
      | Delete single song entry  | 310         | 200    |
