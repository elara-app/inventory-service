# Inventory Service

Spring Boot microservice for inventory item management within a distributed platform.

The repository contains a complete `inventory-service` implementation where delivery quality is visible in the product itself: clear architecture boundaries, strict validation, consistent error contracts, documented APIs, and build-enforced testing standards.

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

Implementation references:
- `src/main/java/com/elara/app/inventory_service/controller/InventoryItemController.java`
- `src/main/java/com/elara/app/inventory_service/service/imp/InventoryItemImp.java`
- `src/main/java/com/elara/app/inventory_service/config/GlobalExceptionHandler.java`
- `src/main/java/com/elara/app/inventory_service/config/OpenApiConfig.java`

## Microservice Ecosystem Context

Inventory Service operates as one service in a broader microservices architecture. Peer services keep their own repositories and documentation, while this repository captures only the interactions required by Inventory:

- **Service discovery and client-side load balancing** for inter-service communication.
- **Centralized configuration** through Config Server.
- **Secrets management** through Vault in the `dev` profile.
- **Distributed config refresh** through Spring Cloud Bus (AMQP).
- **UOM dependency validation** via `UomServiceClientImp` and `@LoadBalanced RestTemplate`.

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

## Technical Highlights

- Layered internal design across `controller`, `service`, `repository`, `mapper`, `exceptions`, and `dto` packages.
- DTO-first API boundaries (records), MapStruct-based mapping, and transactional service methods.
- Centralized exception handling with structured error responses and standard error codes.
- Multi-layer testing strategy (controller, service, repository, mapper, exceptions, utilities).
- Mock isolation patterns (`@AfterEach` + `reset(...)`) with Given-When-Then test structure.
- JaCoCo quality gates enforced in Maven build lifecycle.

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


## Related Documentation

- `AGENTS.md` - coding conventions, architecture patterns, and operational rules
- `TESTING_GUIDE.md` - detailed testing practices and examples
