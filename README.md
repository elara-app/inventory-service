# Inventory Service

Production-style Spring Boot microservice for inventory item management in a distributed platform.

This repository presents a complete `inventory-service` implementation where internal engineering quality is treated as a first-class product feature: layered architecture, strict validation, consistent error handling, API documentation, and multi-layer testing.

## Project Snapshot

| Area | Details |
|------|---------|
| Language & Runtime | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Build Tool | Maven (`./mvnw`) |
| Data Layer | Spring Data JPA + PostgreSQL (runtime) + H2 (tests) |
| API | REST + OpenAPI (springdoc) |
| Mapping | MapStruct |
| Service Discovery / Config | Eureka Client, Config Server, Vault |
| Config Refresh | Spring Cloud Bus (AMQP) |
| Quality Gates | JaCoCo (80% line, 70% branch, zero uncovered classes at bundle level with project exclusions) |

## What This Service Delivers

- Full lifecycle management for inventory items (`create`, `update`, `delete`, `findById`, paginated `findAll`, paginated name search).
- Strict request validation for IDs, text fields, and numeric values.
- Business safeguards such as case-insensitive name uniqueness and external UOM existence verification.
- Stable error contract through centralized exception handling and structured error responses.
- OpenAPI documentation with reusable schemas and example payloads.

Primary implementation references:
- `src/main/java/com/elara/app/inventory_service/controller/InventoryItemController.java`
- `src/main/java/com/elara/app/inventory_service/service/imp/InventoryItemImp.java`
- `src/main/java/com/elara/app/inventory_service/config/GlobalExceptionHandler.java`
- `src/main/java/com/elara/app/inventory_service/config/OpenApiConfig.java`

## Architecture and Internal Design

The service follows a clear layered structure:

- `controller/`: HTTP contract, validation boundaries, response codes, OpenAPI annotations.
- `service/interfaces` and `service/imp`: transactional business logic and orchestration.
- `repository/`: JPA persistence and query operations.
- `mapper/`: MapStruct DTO/entity transformations.
- `exceptions/` + `config/GlobalExceptionHandler`: standardized exception-to-response mapping.
- `dto/` (records): request/response/update contract models.

Package root:
- `src/main/java/com/elara/app/inventory_service`

## Microservice Ecosystem Context

This service is one component in a broader microservices architecture; peer services are documented in their own repositories.

In this repository, the concrete integration points are:

- **Service discovery:** Eureka client dependency and load-balanced inter-service calls.
- **Centralized configuration:** Config Server import (`application.yml`).
- **Secrets management:** Vault import for the `dev` profile (`application-dev.yml`).
- **Distributed refresh:** Spring Cloud Bus via AMQP.
- **Cross-service dependency:** UOM validation through `UomServiceClientImp` using `@LoadBalanced RestTemplate`.

Key references:
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/java/com/elara/app/inventory_service/config/AppConfig.java`
- `src/main/java/com/elara/app/inventory_service/service/imp/UomServiceClientImp.java`

## API Surface

Base path: `item/`

- `POST /item/` - create inventory item
- `GET /item/{id}` - retrieve by id
- `GET /item/` - paginated listing
- `GET /item/search?name=...` - paginated name search
- `PUT /item/{id}` - update by id
- `DELETE /item/{id}` - delete by id

Detailed request/response schemas and examples are configured in:
- `src/main/java/com/elara/app/inventory_service/config/OpenApiConfig.java`
- `src/main/resources/examples/`

## Engineering Quality Signals

- Multi-layer test strategy across controller, service, repository, mapper, exceptions, and utilities.
- Mock isolation patterns (`@AfterEach` + `reset(...)`) and Given-When-Then structure.
- JaCoCo enforcement at build time with explicit thresholds and exclusions.
- Strong conventions for logging nomenclature, exception hierarchy, and DTO-first API boundaries.

References:
- `TESTING_GUIDE.md`
- `AGENTS.md`
- `pom.xml`

## Build, Test, Run

```bash
./mvnw clean install
./mvnw test
./mvnw clean verify
./mvnw spring-boot:run
```

Targeted test examples:

```bash
./mvnw test -Dtest=InventoryItemControllerTest
./mvnw test -Dtest=InventoryItemImpTest#save_withValidRequest_createsAndReturnsResponse
```

## Current State

The repository contains a fully implemented inventory microservice with production-style conventions, documented API behavior, and quality controls integrated into the build lifecycle.

## Related Documentation

- `AGENTS.md` - coding conventions, architecture patterns, and operational rules
- `TESTING_GUIDE.md` - detailed testing practices and examples
