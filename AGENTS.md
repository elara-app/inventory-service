# Agent guidelines — inventory-service

Durable instructions for coding agents modifying this repository. Source code and configuration win over any documentation claim.

## Read first

- [README.md](README.md) — API contract, run/verify, troubleshooting. The README owns the API table.
- [TESTING_GUIDE.md](TESTING_GUIDE.md) — test styles, isolation, coverage in detail.
- [infrastructure/README.md](https://github.com/elara-app/infrastructure#readme) — canonical full-stack commands; this repo never duplicates them.
- [centralized-configuration/README.md](https://github.com/elara-app/centralized-configuration#readme) — profile matrix, property ownership, Vault paths.
- [pom.xml](pom.xml) — versions, dependencies, JaCoCo rules.

## Scope and boundaries

- Owns item master data only: `InventoryItem` (name, description, base UOM id, standard cost, units per purchase UOM, reorder point).
- Does not own stock levels, movements, purchasing, UOM definitions or conversion. There is no cross-service database foreign key.
- The HTTP surface is the single `item/` controller. No security module, no domain events.

## Quick facts

| Fact | Value |
|---|---|
| Package | `com.elara.app.inventory_service` |
| Service ID | `inventory-service` |
| Port | 8081 (`${SERVER_PORT:8081}` in central config; local YAML has no port) |
| Artifact | `com.elara.app:inventory-service:1.3.1` |
| Runtime dependencies | Eureka client, optional Config Server import, optional Vault (dev), RabbitMQ + Spring Cloud Bus, `unit-of-measure-service` |
| Datastore | PostgreSQL `inventory_db` (runtime); H2 in-memory (tests) |

## Commands

Always run `sh ./mvnw` — the wrapper file is mode 644, so an executable invocation fails. Never rely on `./mvnw`, and never `chmod +x` it.

```bash
sh ./mvnw test                                             # all tests (H2 test profile)
sh ./mvnw test -Dtest=InventoryItemControllerTest          # focused class
sh ./mvnw test -Dtest=InventoryItemImpTest#save_withValidRequest_createsAndReturnsResponse
sh ./mvnw -B clean verify -Dspring.profiles.active=test    # full gate: tests + JaCoCo check
```

Full-stack startup is owned by [infrastructure/README.md](https://github.com/elara-app/infrastructure#readme). Do not add compose commands here.

`spring-boot:run` is not a standalone quickstart: the default `dev` profile needs Config Server and PostgreSQL. The `test` profile is not a host-run option either because H2 is test-scoped; `spring-boot:run -Dspring-boot.run.profiles=test` fails with `Cannot load driver class: org.h2.Driver`. Run the full stack via infrastructure, or supply Config Server and PostgreSQL.

## Repository map

| Package | Role | Key classes |
|---|---|---|
| `controller` | HTTP boundary, status codes, validation, OpenAPI annotations | `InventoryItemController` |
| `service/interfaces` | Service contracts | `InventoryItemService`, `UomServiceClient` |
| `service/imp` | Business rules, transactions, UOM check | `InventoryItemImp`, `UomServiceClientImp` |
| `repository` | Spring Data JPA plus bulk delete query | `InventoryItemRepository` |
| `model` | Single JPA entity `inventory_item` | `InventoryItem` |
| `mapper` | MapStruct, `componentModel="spring"`, update ignores id | `InventoryItemMapper` |
| `dto/{request,update,response}` | API records; entities are never exposed | `InventoryItemRequest`, `InventoryItemUpdate`, `InventoryItemResponse`, `UomResponse` |
| `config` | `@LoadBalanced RestTemplate`, OpenAPI, error handling | `AppConfig`, `OpenApiConfig`, `GlobalExceptionHandler`, `ErrorResponse` |
| `exceptions` | Concrete `BaseException` plus specific subclasses | `BaseException`, `ResourceNotFoundException`, `ResourceConflictException` |
| `utils` | Messages, error codes, context holder | `MessageService`, `ErrorCode`, `ApplicationContextHolder` |

## Contracts and invariants

- Create uniqueness is case-insensitive (`existsByNameIgnoreCase`). The DB `unique` constraint is case-sensitive, so application check and constraint can disagree under a create/create race.
- Update skips the uniqueness check only when the exact current name is submitted; a case-only rename returns 409/1003.
- UOM verification: `UomServiceClientImp.verifyUomById` calls `GET http://unit-of-measure-service/{id}` through the load-balanced `RestTemplate`. Only `HttpClientErrorException.NotFound` maps to 404/1004; all other failures propagate to 500/1006. No retry, timeout or circuit breaker.
- No cross-service FK: `baseUnitOfMeasureId` is a plain `Long`.
- Delete is unconditional (bulk JPQL `deleteByIdReturningCount`); 0 affected rows becomes 404/1004. No reference check, no 409 "item in use".
- Transactions: `save`, `update`, `deleteById` are `@Transactional`; reads are `@Transactional(readOnly = true)`. `update` relies on JPA dirty checking and never calls `repository.save`.
- No default pagination sort (`@PageableDefault(size = 20)` only). An unknown sort property is not validated and becomes 500/1006.
- Page JSON is `content` plus `page{size,number,totalElements,totalPages}` via `PagedModel` (`VIA_DTO`). Never document a flat Spring `Page`.
- Exceptions are constructed with a single message string: `new ResourceNotFoundException(message)`, `new ResourceConflictException(message)`. `BaseException` is concrete; `new BaseException(ErrorCode.X, message)` works and is tested.
- `ErrorCode` 1001 = `DATABASE_ERROR` (HTTP 500; the integrity handler returns HTTP 409 with code 1001). 1005 = `SERVICE_UNAVAILABLE` (503) is declared but never thrown by current code.

## Configuration and runtime

- `application.yml`: `spring.config.import: optional:configserver:${CONFIG_URI:http://localhost:8888}` with `fail-fast: false` and retry; default profile `dev`.
- `application-dev.yml`: optional Vault import `secret/inventory-service/dev`; `ddl-auto: update`; deferred seed `db/dev/seed.sql` (10 rows, ids 1–10; UOM ids 1/2 match the UOM seed).
- `application-test.yml`: H2, `create-drop`, Config Server/Vault/Bus/discovery disabled.
- Central config supplies port 8081, datasource, Eureka, RabbitMQ and actuator exposure. The `prod` file is a placeholder, not deployable production configuration.
- Bus/RabbitMQ only propagate config refresh; there are no `@RabbitListener`, `RabbitTemplate` or custom publishers in `src/main`.
- Compose env contract: `SERVER_PORT`, `CONFIG_URI`, `EUREKA_URI`, `SPRING_DATASOURCE_*`, `SPRING_RABBITMQ_*`, `VAULT_URI`, `VAULT_TOKEN`, `SPRING_CLOUD_VAULT_TOKEN`.

## Testing and quality gates

- `sh ./mvnw test` runs the suite on the `test` profile (H2).
- Controller: `InventoryItemControllerTest` uses `@WebMvcTest(InventoryItemController.class)`, `MockMvc`, `@MockitoBean`.
- Service: `InventoryItemImpTest` uses `@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`, and resets all four mocks in `@AfterEach`.
- Repository: `InventoryItemRepositoryTest` uses `@DataJpaTest` plus `TestEntityManager`.
- Mapper: `InventoryItemMapperTest` uses `@SpringBootTest` and `@ActiveProfiles("test")`.
- Also present: `UomServiceClientImpTest`, `GlobalExceptionHandlerLoggingTest`, `CustomExceptionsTest`, `MessageServiceTest`, `ApplicationContextHolderTest`, `InventoryServiceApplicationTests`.
- JaCoCo at `verify`: bundle line ≥ 0.80, branch ≥ 0.70, missed classes 0; package line ≥ 0.70 excluding `config.*` and the application class; global excludes `**/config/**`, `**/dto/**`, `**/InventoryServiceApplication.class`. The report runs in the `test` phase.
- CI runs the reusable org test workflow on PRs to `develop`/`main` and publishes the image on `main`.

## Definition of done

- Source compiles and `sh ./mvnw -B clean verify -Dspring.profiles.active=test` passes with the JaCoCo check.
- Docs updated when behavior, endpoints, statuses, config keys or commands change; README, AGENTS, TESTING_GUIDE and copilot instructions stay consistent.
- No API contract drift: status codes, error codes and the nested page shape stay unchanged unless the change is intentional and documented.
- New tests cover new branches; DTO/config classes are coverage-exempt, controllers and services are not.

## Stop and ask

- Changing the HTTP API contract (methods, paths, statuses, error codes, page shape).
- Changing cross-service contracts (UOM client behavior, service id, gateway route, seed id contract 1–2).
- Schema or database changes (`ddl-auto`, columns, constraints, seed data).
- Secrets, Vault paths, config import behavior or the profile matrix.
- Introducing runtime dependencies such as retry, circuit breaker, HTTP client or migrations.

## Known traps

- `findByNameIgnoreCase` does not exist; the repository has only `existsByNameIgnoreCase` and `findAllByNameContainingIgnoreCase`.
- Exception constructors are single-message only; there is no `(entity, field, value)` form, and `BaseException` is concrete, not abstract.
- Delete has no reference check; OpenAPI text and `examples/error-delete-conflict.json` claiming a 409 for "referenced by records" are wrong.
- The OpenAPI page schema and `examples/inventory-item-page.json` are flat and stale; live responses return the nested page.
- `ddl-auto: update` in dev can drift the schema; there are no migrations.
- The UOM client maps only 404; a UOM outage is 500/1006, not 503/1005.
- `@PageableDefault` sets no sort; do not add or document a `sort=name` default.
- The wrapper is mode 644: use `sh ./mvnw`, not an executable invocation, and do not `chmod +x`.

## Documentation maintenance

- README owns the API table and quickstart; this file owns constraints and traps; TESTING_GUIDE owns test detail. Do not duplicate the API table here.
- Keep `.github/copilot-instructions.md` a short pointer to this file.
- Update this file when invariants, commands, gates or traps change.
