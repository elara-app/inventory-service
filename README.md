[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/elara-app/inventory-service)

# Inventory Service

`inventory-service` owns the item master data of the Elara platform: item names and descriptions, the base unit of measure reference, standard cost, units per purchase UOM and reorder point. It does not own stock levels, stock movements, purchasing documents or unit-of-measure definitions; those belong to other services, and UOM data to `unit-of-measure-service`.

## At a glance

| Property | Value |
|---|---|
| Service ID | `inventory-service` |
| Runtime port | 8081 (`SERVER_PORT`; supplied by central config, not by local YAML) |
| Stack | Java 21, Spring Boot 3.5.6, Spring Cloud 2025.0.0, Spring Data JPA, MapStruct 1.6.3, springdoc 2.8.0 |
| Build tool | Maven wrapper — invoke as `sh ./mvnw` (the wrapper file is not executable) |
| Datastore | PostgreSQL 16 (`inventory_db`) at runtime; H2 in-memory for tests |
| Dependencies | Eureka client, optional Config Server, optional Vault (dev), RabbitMQ + Spring Cloud Bus, `unit-of-measure-service` |
| Health | `GET /actuator/health` (details always shown; liveness/readiness groups enabled) |
| Quality gates | JaCoCo enforced by `verify`: line ≥ 80%, branch ≥ 70%, 0 missed classes; package line ≥ 70% |

## Responsibilities and boundaries

- CRUD for inventory items: create, read by id, paginated list, paginated case-insensitive name search, update, delete.
- Validation of required and positive numeric fields, string length limits, and case-insensitive name uniqueness on create.
- Verification that `baseUnitOfMeasureId` exists in `unit-of-measure-service` before create and update.
- Structured error responses for the whole HTTP surface.
- Not owned here: stock ledger, on-hand quantity, stock movements, purchasing, cost accounting, UOM definitions and conversion rates.

## Runtime architecture

A layered MVC service with one domain entity:

- `controller` → `service/imp` → `repository` (Spring Data JPA / PostgreSQL); MapStruct maps DTOs to the `inventory_item` entity.
- `InventoryItemImp` calls `UomServiceClientImp`, a `@LoadBalanced RestTemplate` client that resolves `unit-of-measure-service` through Eureka and calls `GET http://unit-of-measure-service/{id}`.
- Page responses are serialized as `PagedModel` (`@EnableSpringDataWebSupport(... VIA_DTO)`), which produces the nested `content` + `page` shape.
- Platform wiring (gateway route, Eureka, Config Server, Vault, RabbitMQ) lives in [infra/README.md](../infra/README.md) and [centralized-configuration/README.md](../centralized-configuration/README.md).

## API

The controller is mapped at `item/` (the collection path includes the trailing slash). Successful responses carry an `InventoryItemResponse` body.

| Method | Path | Request | Success | Main errors |
|---|---|---|---|---|
| POST | `/item/` | `InventoryItemRequest` JSON | 201 + item | 400 code 1002 validation; 415 code 1002 unsupported/missing content type; 409 code 1003 duplicate name; 404 code 1004 unknown UOM; 409 code 1001 DB integrity; 500 code 1006 |
| GET | `/item/` | `page` (0-based, default 0), `size` (default 20), `sort` (none by default) | 200 + nested page | 500 code 1006 (including an unknown `sort` property) |
| GET | `/item/{id}` | positive `id` path variable | 200 + item | 400 code 1002 (`id` ≤ 0 or non-numeric); 404 code 1004; 500 code 1006 |
| GET | `/item/search` | required `name` (`@NotBlank`), plus pagination | 200 + nested page, case-insensitive `contains` match | 400 code 1002; 500 code 1006 |
| PUT | `/item/{id}` | `InventoryItemUpdate` JSON, all fields required | 200 + updated item | 400 code 1002; 404 code 1004 (item or UOM); 409 code 1003; 500 code 1006 |
| DELETE | `/item/{id}` | positive `id` path variable | 204, empty body | 400 code 1002; 404 code 1004; 500 code 1006 |

Path notes: `/item/search` has no trailing slash. `/item`, `/item/1/` and other unmatched paths fall through to the generic handler and return 500 code 1006.

### Page response shape

The list endpoints return a nested page, never a flat Spring `Page`:

```json
{"content": [{"id": 1, "name": "Steel Bolt M10"}],
 "page": {"size": 20, "number": 0, "totalElements": 10, "totalPages": 1}}
```

`page` contains exactly `size`, `number`, `totalElements` and `totalPages`. There is no default sort; ordering is whatever the database returns. Invalid `page`/`size` values are silently normalized to the defaults.

### Validation

- `name`: required, max 100 characters; create rejects any existing name that matches case-insensitively.
- `description`: optional, max 200 characters.
- `baseUnitOfMeasureId`: required, positive; verified against the UOM service.
- `standardCost`, `unitPerPurchaseUom`, `reorderPointQuantity`: required, greater than 0, max 10 integer and 2 fraction digits.
- Update requires every field. A case-only rename is rejected because the update check skips uniqueness only when the exact current name is submitted.

### UOM verification

`UomServiceClientImp` calls `GET http://unit-of-measure-service/{id}` through the load-balanced `RestTemplate`. Only HTTP 404 becomes code 1004 (`RESOURCE_NOT_FOUND`). Every other 4xx/5xx/network failure propagates and becomes 500 code 1006 (`UNEXPECTED_ERROR`). There is no retry, timeout or circuit breaker, and no cross-service database foreign key; the UOM id is stored as a plain `Long`.

### Delete behavior

Delete is a bulk JPQL delete returning the affected row count; count 0 becomes 404 code 1004. There is no reference check, so it never returns 409 "item in use". A real database integrity failure would surface as HTTP 409 with code 1001.

### Error contract

Errors use the envelope `{code, value, message, timestamp, path}` (`timestamp` is a local date-time without timezone).

| Code | Value | HTTP | Thrown when |
|---|---|---|---|
| 1001 | `DATABASE_ERROR` | 500; the data-integrity handler returns 409 with this code | DB failures; integrity violations |
| 1002 | `INVALID_DATA` | 400; also 415 for unsupported media type and 405 for unsupported method | Bean validation and request-shape errors |
| 1003 | `RESOURCE_CONFLICT` | 409 | Duplicate name on create/update |
| 1004 | `RESOURCE_NOT_FOUND` | 404 | Missing item or UOM |
| 1005 | `SERVICE_UNAVAILABLE` | 503 if ever thrown | Declared only; no current code path throws it |
| 1006 | `UNEXPECTED_ERROR` | 500 | Fallback: non-404 UOM failures, malformed/empty JSON, unmatched paths, bad sort |

## Run and verify

Prerequisites: Java 21. The Maven wrapper is mode 644 — always run it as `sh ./mvnw`.

```bash
# self-contained: the test profile disables config/vault/bus/discovery and uses H2
sh ./mvnw -B clean verify -Dspring.profiles.active=test

# focused tests
sh ./mvnw test -Dtest=InventoryItemControllerTest
sh ./mvnw test -Dtest=InventoryItemImpTest#save_withValidRequest_createsAndReturnsResponse
```

Full-stack startup, ports and logs are owned by [infra/README.md](../infra/README.md#verify-the-stack). Do not start the stack from this repository.

Service smoke command (through the gateway, once the stack is running):

```bash
curl -s http://127.0.0.1:8080/api/v1/inventory/item/ | jq '.page.totalElements'   # 10 (dev seed)
```

Collection path correction: the gateway prefix `/api/v1/inventory` uses `StripPrefix=3` and maps to this service's `item/` controller, so the usable URL is `http://127.0.0.1:8080/api/v1/inventory/item/` — not the prefix alone.

Direct service endpoints (bypassing the gateway):

- Swagger UI through the gateway: `http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI JSON: `http://127.0.0.1:8081/v3/api-docs`
- Swagger UI direct: `http://127.0.0.1:8081/swagger-ui/index.html` (`/swagger-ui.html` redirects)

`sh ./mvnw spring-boot:run` is not a standalone quickstart. The default `dev` profile needs Config Server (port, datasource, JPA dialect) plus PostgreSQL, and optionally Vault. The `test` profile is not a host-run option either: H2 is test-scoped, so `spring-boot:run -Dspring-boot.run.profiles=test` fails with `Cannot load driver class: org.h2.Driver`. Run the full stack via infra, or supply Config Server and PostgreSQL.

## Configuration

Service-specific configuration lives in `src/main/resources/` and in `centralized-configuration/inventory-service-{dev,test,prod}.yml`; the full profile matrix and property ownership are in [centralized-configuration/README.md](../centralized-configuration/README.md).

| Key / file | Purpose |
|---|---|
| `application.yml` | service name; `spring.config.import: optional:configserver:${CONFIG_URI:http://localhost:8888}`; default profile `dev`; config client retry |
| `application-dev.yml` | optional Vault import `secret/inventory-service/dev`; `ddl-auto: update`; dev seed SQL `db/dev/seed.sql` |
| `application-test.yml` | H2 `jdbc:h2:mem:testdb`; `create-drop`; Config Server, Vault, Bus and discovery disabled |
| `uom.service.name` | UOM service id for the load-balanced client; default `unit-of-measure-service` |
| Runtime env | `SERVER_PORT`, `CONFIG_URI`, `EUREKA_URI`, `SPRING_DATASOURCE_*`, `SPRING_RABBITMQ_*`, `VAULT_URI`, `VAULT_TOKEN` / `SPRING_CLOUD_VAULT_TOKEN` |

`inventory-service-prod.yml` is a placeholder, not deployable production configuration. Dev secrets (`service-secrets/inventory-service-dev-secrets.json`) are development-only defaults. RabbitMQ and Spring Cloud Bus exist to propagate config refresh (`refresh`/`busrefresh` actuator endpoints), not as a domain event bus.

## Testing and quality gates

- Tests run on the `test` profile: H2 in-memory and all remote integrations disabled.
- `sh ./mvnw -B clean verify -Dspring.profiles.active=test` enforces JaCoCo: bundle line ≥ 80%, branch ≥ 70%, 0 missed classes; package line ≥ 70% (excluding `config.*` and the application class). Global exclusions: `**/config/**`, `**/dto/**`, `**/InventoryServiceApplication.class`.
- The JaCoCo report is generated in the `test` phase at `target/site/jacoco/index.html`; the coverage `check` runs at `verify`.
- CI: PRs to `develop`/`main` run the reusable org test workflow; pushes to `main` publish `ghcr.io/elara-app/inventory-service`.

See [TESTING_GUIDE.md](TESTING_GUIDE.md) for test styles and examples.

## Current limitations

- Item master only: no stock ledger, on-hand quantity, movements or domain events.
- No audit timestamps or audit log.
- No database-level foreign key to UOM; existence is verified per request only.
- The UOM client has no retry, timeout or circuit breaker; any non-404 failure is reported as 500 code 1006.
- Case-insensitive uniqueness is enforced by the application check while the DB constraint is case-sensitive; concurrent creates can still persist case variants.
- A case-only rename is rejected (409 code 1003) because the update check compares the exact current name.
- Schema is managed with Hibernate `ddl-auto: update` plus dev seed SQL; there are no migrations.
- OpenAPI examples are hand-maintained (`src/main/resources/examples/`) and stale for the page shape and delete conflict; live Swagger and live responses are authoritative.
- `inventory-service-prod.yml` in the config repository contains placeholder properties only.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `GET /item` returns 500 code 1006 | The collection mapping requires `item/` | Use `/item/` with the trailing slash |
| Case-only rename returns 409 code 1003 | The update check compares exact names | Rename to a different name, or change the check deliberately |
| UOM verification returns 500 code 1006 | UOM service down or returned a non-404 error; only 404 is mapped | Start `unit-of-measure-service`; do not expect a retry |
| Missing `name` in search returns 400 code 1002 | `name` is required and `@NotBlank` | Always send `name` |
| `sort=unknownField` returns 500 code 1006 | The sort property is not validated against the entity | Sort only by entity fields or omit `sort` |
| 409 code 1001 instead of 1003 | The DB constraint rejected the write (integrity violation) | Check the item name; create races can produce case variants |
| `spring-boot:run` fails during startup | The `dev` profile needs Config Server + PostgreSQL; the H2 test profile is not on the `spring-boot:run` classpath | Run the stack via infra, or supply Config Server + PostgreSQL |
| Seed data missing | The seed SQL runs only in the `dev` profile | Check the active profile and container logs |

## Documentation

- [AGENTS.md](AGENTS.md) — agent commands, invariants, traps
- [TESTING_GUIDE.md](TESTING_GUIDE.md) — testing patterns and real examples
- [infra/README.md](../infra/README.md) — full-stack startup and verification
- [centralized-configuration/README.md](../centralized-configuration/README.md) — profiles, properties, Vault paths
- Live API: Swagger UI `http://127.0.0.1:8080/swagger-ui.html`; direct `http://127.0.0.1:8081/swagger-ui/index.html`
