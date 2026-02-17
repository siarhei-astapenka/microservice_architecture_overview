# Contract Testing with Spring Cloud Contract

## Overview

This document describes the contract testing strategy implemented using **Spring Cloud Contract** for the microservice architecture. Contract tests ensure that service APIs adhere to agreed contracts, enabling independent service evolution without breaking integrations.

## Architecture

```
resource-service  ──(RabbitMQ)──►  resource-processor  ──(HTTP POST /songs)──►  song-service
      │                                    │
      └──────────────(HTTP GET /resources/{id})──────────────────────────────────────────────┘
```

### Communication Contracts

| Producer         | Consumer             | Protocol | Contract                                |
|------------------|----------------------|----------|-----------------------------------------|
| song-service     | resource-processor   | HTTP     | `POST /songs` — save song metadata      |
| song-service     | resource-processor   | HTTP     | `GET /songs/{id}` — get song metadata   |
| resource-service | resource-processor   | HTTP     | `GET /resources/{id}` — download binary |
| resource-service | resource-processor   | AMQP     | `ResourceUploadMessage` via RabbitMQ    |

---

## Project Structure

### Contract Files (Groovy DSL)

```
song-service/src/test/resources/contracts/song-service/http/
├── shouldSaveSongMetadata.groovy          # POST /songs contract
└── shouldGetSongMetadataById.groovy       # GET /songs/{id} contract

resource-service/src/test/resources/contracts/resource-service/
├── http/
│   └── shouldGetResourceById.groovy       # GET /resources/{id} contract
└── messaging/
    └── shouldSendResourceUploadMessage.groovy  # RabbitMQ message contract
```

### Producer Test Base Classes

```
song-service/src/test/java/.../contract/
└── SongServiceContractBase.java           # @WebMvcTest base for HTTP contracts

resource-service/src/test/java/.../contract/
├── ResourceServiceHttpContractBase.java   # @WebMvcTest base for HTTP contracts
└── ResourceServiceMessagingContractBase.java  # @SpringBootTest + @AutoConfigureMessageVerifier
```

### Consumer Tests (resource-processor)

```
resource-processor/src/test/java/.../contract/
├── SongServiceContractConsumerTest.java           # Consumes song-service HTTP stubs
├── ResourceServiceContractConsumerTest.java       # Consumes resource-service HTTP stubs
└── ResourceUploadMessageContractConsumerTest.java # Consumes resource-service messaging stubs
```

---

## How It Works

### Producer Side (song-service, resource-service)

1. **Contracts** are defined as Groovy DSL files in `src/test/resources/contracts/`
2. The `spring-cloud-contract-maven-plugin` generates test classes from contracts during `generate-test-sources` phase
3. Generated tests extend the **base class** which provides `MockMvc` setup and service mocks
4. Tests verify that the actual service implementation matches the contract
5. On `mvn install`, a **stubs JAR** (`*-stubs.jar`) is published to the local Maven repository

### Consumer Side (resource-processor)

1. `@AutoConfigureStubRunner` loads stubs from the local Maven repository
2. WireMock server starts on the configured port, serving HTTP stubs
3. For messaging contracts, `StubTrigger` sends messages to Spring Integration channels
4. Consumer tests verify that the client code correctly communicates with the stub

---

## Running Contract Tests

### Prerequisites

Java 17 and Maven must be available. The commands below use the system Maven.

### Step 1 — Build and Publish Producer Stubs

Producer stubs must be installed to the local Maven repository before running consumer tests.

```bash
# Publish song-service stubs (HTTP contracts)
mvn -f song-service/pom.xml clean install -DskipTests

# Publish resource-service stubs (HTTP + messaging contracts)
mvn -f resource-service/pom.xml clean install -DskipTests
```

This creates:
- `~/.m2/repository/com/epam/learn/song-service/3.5.3/song-service-3.5.3-stubs.jar`
- `~/.m2/repository/com/epam/learn/resource-service/3.5.3/resource-service-3.5.3-stubs.jar`

### Step 2 — Run Producer Contract Tests (Provider Verification)

These tests verify that the actual service implementation satisfies the contracts.

```bash
# song-service: verifies POST /songs and GET /songs/{id}
mvn -f song-service/pom.xml test \
  -Dtest="com.epam.learn.song_service.contract.HttpTest"

# resource-service: verifies GET /resources/{id} (HTTP) and ResourceUploadMessage (messaging)
mvn -f resource-service/pom.xml test \
  -Dtest="org.springframework.cloud.contract.verifier.tests.HttpTest,org.springframework.cloud.contract.verifier.tests.MessagingTest"
```

### Step 3 — Run Consumer Contract Tests

These tests verify that the consumer (resource-processor) correctly uses the APIs defined in the contracts.

```bash
# All three consumer contract tests
mvn -f resource-processor/pom.xml test \
  -Dtest="SongServiceContractConsumerTest,ResourceServiceContractConsumerTest,ResourceUploadMessageContractConsumerTest"
```

### Full Contract Test Cycle (One Command)

```bash
# 1. Publish stubs from producers
mvn -f song-service/pom.xml clean install -DskipTests && \
mvn -f resource-service/pom.xml clean install -DskipTests && \
# 2. Run all tests in resource-processor (includes consumer contract tests)
mvn -f resource-processor/pom.xml clean test
```

### Run All Tests in All Services

```bash
mvn -f song-service/pom.xml clean install && \
mvn -f resource-service/pom.xml clean install && \
mvn -f resource-processor/pom.xml clean test
```

> **Note**: The `install` goal for producers runs all tests AND publishes stubs. The consumer tests in `resource-processor` use `StubsMode.LOCAL` and require stubs to be present in `~/.m2` before running.

---

## Test Results

| Service           | Total Tests | Contract Tests                          |
|-------------------|-------------|-----------------------------------------|
| song-service      | 69 ✅        | 2 producer (HTTP: POST + GET)           |
| resource-service  | 77 ✅        | 2 producer (HTTP + messaging)           |
| resource-processor| 61 ✅        | 3 consumer (2 HTTP + 1 messaging)       |

---

## Technical Details

### Messaging Contract Implementation

The messaging contract for `resource-service` uses a **Spring Integration bridge** pattern:

1. `ResourceServiceMessagingContractBase` creates a `QueueChannel` bean named `resource.exchange`
2. The `AmqpTemplate` mock is configured to forward messages to this channel
3. `ContractVerifierMessaging` (Spring Integration-based) receives messages from the channel
4. The generated `MessagingTest` calls `triggerResourceUploadMessage()` → verifies message structure

### Stubs Propagation

Stubs are propagated via the local Maven repository (`~/.m2`). In a CI/CD pipeline, stubs should be published to a shared artifact repository (Nexus/Artifactory) and consumers should use `StubsMode.REMOTE` or `StubsMode.CLASSPATH`.

### WireMock Ports

Consumer tests use fixed ports for WireMock stubs:
- `song-service` stubs: port **8081**
- `resource-service` stubs: port **8080**

To avoid port conflicts when running tests in parallel, consider switching to random ports by removing the port from the `ids` parameter in `@AutoConfigureStubRunner`.
