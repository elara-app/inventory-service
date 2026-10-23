# Testing Guide — inventory-service

Focused guide for writing, running and debugging tests in this repository. All class and method names below come from `src/test/java`; run every command from the repository root.

## Testing philosophy and pyramid

Tests here follow a small pyramid:

- **Unit tests** (fastest, most numerous): business rules with collaborators mocked. `InventoryItemImpTest`, `UomServiceClientImpTest`, `CustomExceptionsTest`, `MessageServiceTest`, `ApplicationContextHolderTest`.
- **Web slice tests**: the HTTP contract of the controller with the service mocked. `InventoryItemControllerTest`.
- **Persistence slice tests**: JPA mappings and derived queries against H2. `InventoryItemRepositoryTest`.
- **Integration tests**: a full application context where the mapper bean must be real. `InventoryItemMapperTest`, `InventoryServiceApplicationTests`.

Guidelines:

- One behavior per test; structure with Given-When-Then comments.
- Name tests `method_condition_expectedResult` (for example `save_withDuplicateName_throwsResourceConflictException`) and add a readable `@DisplayName`.
- Assert behavior: returned values, thrown exceptions and serialized JSON. Use `verify(...)` only for meaningful collaboration checks.
- Cover happy paths, edge cases (empty results, boundary lengths, minimum decimals) and error paths (conflicts, not found, validation failures).
- Use `@Nested` classes to group operations, as `InventoryItemImpTest` does with `SaveTests`, `UpdateTests`, `DeleteTests`, `FindByIdTests`, `FindAllTests` and `FindAllByNameTests`.

## Test classes and styles

| Class | Style | Annotations / tools |
|---|---|---|
| `InventoryItemImpTest` | Service unit tests with Mockito | `@ExtendWith(MockitoExtension.class)`, four `@Mock` fields (`InventoryItemMapper`, `InventoryItemRepository`, `MessageService`, `UomServiceClient`), `@InjectMocks InventoryItemImp service` |
| `UomServiceClientImpTest` | Client unit tests with Mockito | `@Mock RestTemplate`, `@Mock MessageService`, `@InjectMocks UomServiceClientImp`; sets the `@Value` field with `ReflectionTestUtils.setField(uomServiceClient, "uomServiceName", ...)` |
| `InventoryItemControllerTest` | Web slice | `@WebMvcTest(InventoryItemController.class)`, `@ActiveProfiles("test")`, `MockMvc`, `@MockitoBean` for `InventoryItemService` and `MessageService`, `@Autowired ObjectMapper` |
| `InventoryItemRepositoryTest` | JPA slice | `@DataJpaTest`, `@ActiveProfiles("test")`, `@Autowired TestEntityManager` |
| `InventoryItemMapperTest` | Spring integration | `@SpringBootTest`, `@ActiveProfiles("test")`, `@Autowired InventoryItemMapper` |
| `GlobalExceptionHandlerLoggingTest` | Unit tests for logging | `@ExtendWith(MockitoExtension.class)`, Logback `ListAppender<ILoggingEvent>` attached to `GlobalExceptionHandler` |
| `CustomExceptionsTest` | Plain JUnit | No extension; instantiates `BaseException`, `ResourceNotFoundException` and other subclasses directly |
| `MessageServiceTest` | Unit tests with Mockito | `@Mock MessageSource`, `@InjectMocks MessageService` |
| `ApplicationContextHolderTest` | Unit tests with Mockito | Builds `new ApplicationContextHolder()` per test; resets the mocked `ApplicationContext` |
| `InventoryServiceApplicationTests` | Context load | `@SpringBootTest`, `@ActiveProfiles("test")`, one `contextLoads` test |

## Structure example

The repository uses JUnit 5 with `@Nested` groups and readable display names:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryItemImp Service Tests")
class InventoryItemImpTest {

    @Mock private InventoryItemMapper mapper;
    @Mock private InventoryItemRepository repository;
    @Mock private MessageService messageService;
    @Mock private UomServiceClient uomServiceClient;
    @InjectMocks private InventoryItemImp service;

    @AfterEach
    void tearDown() {
        reset(mapper, repository, messageService, uomServiceClient);
    }

    @Nested
    @DisplayName("Save Operation Tests")
    class SaveTests {

        @Test
        @DisplayName("save_withValidRequest_createsAndReturnsResponse")
        void save_withValidRequest_createsAndReturnsResponse() {
            // Given / When / Then
        }
    }
}
```

## What to test at each layer

| Layer | Test class | What it proves |
|---|---|---|
| Business rules | `InventoryItemImpTest` | Validation order, name uniqueness, UOM verification, transaction methods, mapping delegation |
| Remote client | `UomServiceClientImpTest` | 404 mapping, URL construction, failure propagation, no swallowing of errors |
| HTTP contract | `InventoryItemControllerTest` | Status codes, Bean Validation on path/body, error envelope, nested page serialization |
| Persistence | `InventoryItemRepositoryTest` | Derived queries, case-insensitive search, bulk delete count |
| Mapping | `InventoryItemMapperTest` | Field-by-field DTO mapping, null handling, precision, id preservation on update |
| Logging | `GlobalExceptionHandlerLoggingTest` | WARN for 4xx, ERROR with throwable for 5xx, no row values in integrity logs |

## Service unit test patterns

`InventoryItemImpTest` documents the exact service order: name uniqueness first, then UOM verification, then mapping and persistence.

- `save_withValidRequest_createsAndReturnsResponse` stubs `repository.existsByNameIgnoreCase(...)` to `false`, `doNothing().when(uomServiceClient).verifyUomById(1L)`, `mapper.toEntity`, `repository.save` and `mapper.toResponse`, then asserts the returned `InventoryItemResponse`.
- `save_withDuplicateName_throwsResourceConflictException` stubs `existsByNameIgnoreCase` to `true`, asserts `ResourceConflictException`, and verifies the UOM client, mapper and repository are never called.
- `save_withInvalidUomId_throwsResourceNotFoundException` makes `verifyUomById` throw `ResourceNotFoundException` and verifies `mapper.toEntity` and `repository.save` are never called.
- `save_withNullRequest_throwsNullPointerException` documents that the service does not null-guard its input; over HTTP this path is unreachable because `@Valid @RequestBody` rejects the request first.
- `update_withValidData_updatesAndReturnsResponse` stubs `repository.findById`, a `false` uniqueness check for the new name, `verifyUomById`, `mapper.updateEntityFromDto` and `mapper.toResponse`.
- `update_withSameName_updatesWithoutNameCheck` submits the exact current name and verifies `existsByNameIgnoreCase` is never called.
- `update_withNonExistentId_throwsResourceNotFoundException` and `update_withDuplicateName_throwsResourceConflictException` assert the exception and verify the UOM client is not called.
- `deleteById_withExistingId_deletesSuccessfully` stubs `repository.deleteByIdReturningCount(id)` to `1`; `deleteById_withNonExistentId_throwsResourceNotFoundException` stubs `0` and asserts the message.
- `findAll_withData_returnsPagedResponse`, `findAll_withEmptyDatabase_returnsEmptyPage`, `findAllByName_withMatchingResults_returnsPagedResponse` and `findAllByName_caseInsensitive_returnsMatches` use `PageImpl` + `PageRequest.of(...)` and assert `getContent()` / `getTotalElements()`.
- `findById_withExistingId_returnsResponse` stubs `repository.findById(id)` with an entity and asserts the mapped response; `findById_withNonExistentId_throwsResourceNotFoundException` stubs `Optional.empty()` and asserts the exception.

Stub `messageService.getMessage(...)` whenever a test asserts an exception message; because `MessageService.getMessage` is varargs, the tests stub both `getMessage(anyString(), anyString())` and `getMessage(anyString(), anyString(), anyString(), anyString())`.

### Assertion style

- Use AssertJ throughout: `assertThat(result.name()).isEqualTo("Steel Bolt M10")`.
- Use `assertThatThrownBy(() -> service.save(request)).isInstanceOf(ResourceConflictException.class).hasMessage(errorMessage)` for error paths.
- Use `assertThatCode(() -> uomServiceClient.verifyUomById(id)).doesNotThrowAnyException()` for success paths that return nothing.
- Use `verify(mock, never()).method(...)` to prove a step was skipped, for example that the UOM client is not called when the name already exists.

## Remote client test patterns

`UomServiceClientImpTest` covers the behavior that matters for failure classification:

- `verifyUomById_withValidId_completesSuccessfully` stubs `restTemplate.getForEntity(any(URI.class), eq(UomResponse.class))` and verifies `messageService.getMessage("crud.read.success", "Uom")`.
- `verifyUomById_withNonExistentId_throwsResourceNotFoundException` and `verifyUomById_with404NotFound_throwsResourceNotFoundException` throw `HttpClientErrorException.NotFound.create(...)` and assert `ResourceNotFoundException` with the `UOM` message.
- `verifyUomById_withServiceUnavailable_throwsException` throws a plain `RuntimeException` and asserts it propagates untouched; the client does not translate non-404 failures.
- `verifyUomById_withDifferentServiceName_constructsCorrectUrl` sets `uomServiceName` with `ReflectionTestUtils.setField` and verifies the URI contains the custom service name and the id.
- `verifyUomById_withMultipleValidIds_successfullyVerifiesAll` chains three responses and verifies `times(3)` calls; `verifyUomById_withDefaultServiceName_usesCorrectDefault` checks the fallback service id.

Because `@Value` happens at injection time, the field must be set through `ReflectionTestUtils` before invoking the client.

## Controller and OpenAPI notes

`MockMvc` performs requests against the real controller and the real `GlobalExceptionHandler` (`@ControllerAdvice` is picked up by `@WebMvcTest`). Examples:

```java
mockMvc.perform(get("/item/"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.content").isArray())
    .andExpect(jsonPath("$.page", hasEntry("totalElements", 2)));
```

- Success paths assert the nested page shape: `$.content` and `$.page` with `size`, `number`, `totalElements` and `totalPages`. The flat Spring `Page` fields never appear because `@EnableSpringDataWebSupport(... VIA_DTO)` serializes through `PagedModel`.
- Existing validation tests assert only `status()`; no test asserts `jsonPath("$.code")` or `$.value` yet. When extending an error test, add those assertions with the expected pairs: 400/1002 for invalid bodies and ids, 404/1004 for missing items or UOMs, 409/1003 for name conflicts, 415/1002 for a wrong content type.
- `create_withValidRequest_returnsCreated201`, `getById_withNonNumericId_returnsBadRequest400`, `search_withBlankName_returnsBadRequest400`, `update_withMissingFields_returnsBadRequest400` and `delete_withExistingId_returnsNoContent204` are the canonical examples per operation.
- Pagination is passed with `.param("page", "1").param("size", "5")`; the default is `size = 20` with no sort.
- `getAll_withInvalidSortField_returnsOk200WithDefaultSort` passes because the service is mocked and returns a fake `Page` unconditionally. With a real repository an unknown sort property returns 500/1006; do not treat that test name as live behavior.
- The OpenAPI example JSONs in `src/main/resources/examples/` are hand-maintained and not asserted by tests. The page example is stale (flat shape); when the contract changes, update `OpenApiConfig` and the examples, then rely on live Swagger as the source of truth.

## Repository and H2 notes

`InventoryItemRepositoryTest` persists through `TestEntityManager`:

```java
entityManager.persist(item);
entityManager.flush();
```

- Query methods under test: `existsByNameIgnoreCase`, `findAllByNameContainingIgnoreCase`, `deleteByIdReturningCount`.
- `existsByNameIgnoreCase_withExactMatch_returnsTrue`, `existsByNameIgnoreCase_withDifferentCase_returnsTrue` (lower, upper and mixed case) and `existsByNameIgnoreCase_withNonExistentName_returnsFalse` cover uniqueness.
- `findAllByNameContainingIgnoreCase_withPartialMatch_returnsMatchingItems`, `_caseInsensitive_returnsMatchingItems`, `_withNoMatches_returnsEmptyPage` and `_withPagination_returnsCorrectPage` cover search.
- `deleteByIdReturningCount_withMultipleItems_onlyDeletesTargeted` asserts the bulk JPQL delete affects only the target row.
- H2 runs as `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` with `ddl-auto: create-drop` (`application-test.yml`), so the schema is created from the entity on startup and dropped on context shutdown.
- Do not assert PostgreSQL-specific behavior (SQL state, sequences, dialect functions) in H2 tests; verify that against the running stack instead.

## Mapping and exception tests

- `InventoryItemMapperTest` covers `toEntity` (all fields, null id, null description, decimal precision), `toResponse` (all fields, null description, precision), `updateEntityFromDto` (all fields, id preserved, null and empty description) and boundary values (100-char name, 200-char description, `0.01` decimals).
- `CustomExceptionsTest` instantiates `BaseException` directly, proving it is concrete, and checks `getCode()` / `getValue()` for each subclass, for example `constructor_withErrorCodeAndMessage_setsFieldsCorrectly`.
- `GlobalExceptionHandlerLoggingTest` attaches a Logback `ListAppender` to the handler: `baseExceptionWith4xxLogsWarnWithoutThrowable`, `baseExceptionWith5xxLogsErrorWithThrowable`, `dataIntegrityViolationLogsWarnWithoutRowValues` (the log contains the SQL state but not the row value) and `unexpectedExceptionLogsErrorWithThrowable`.
- `MessageServiceTest` verifies key lookup and fallback behavior through the mocked `MessageSource`; `ApplicationContextHolderTest` verifies the static holder setter.

## Isolation rules

- Reset mocks in `@AfterEach`. `InventoryItemImpTest` calls `reset(mapper, repository, messageService, uomServiceClient)`; `InventoryItemControllerTest` calls `reset(service)`; `UomServiceClientImpTest` calls `reset(restTemplate, messageService)`.
- Never share mutable state between tests. Helpers such as `createStandardRequest()`, `createStandardEntity()` and `createStandardResponse()` build fresh objects per test.
- Repository tests rely on `@DataJpaTest` transactions; each test method rolls back. Persist distinct item names per test because `name` is `unique = true`.
- `ApplicationContextHolder` holds a `static ApplicationContext`; `ApplicationContextHolderTest` constructs a local instance instead of relying on the Spring context.
- Use `@ActiveProfiles("test")` for anything that boots Spring; it disables Config Server, Vault, Bus and discovery, and switches to H2.

## Mocking strategy

- Mock at boundaries: `InventoryItemRepository`, `InventoryItemMapper` (in service tests), `UomServiceClient` and `MessageService` in service tests; `InventoryItemService` and `MessageService` in controller tests.
- Stub only what the path under test uses; unused stubs fail strict Mockito with `UnnecessaryStubbingException`.
- `UomServiceClientImpTest` stubs `restTemplate.getForEntity(any(URI.class), eq(UomResponse.class))`; the 404 case throws `HttpClientErrorException.NotFound`, and generic failures are expected to propagate.
- Use `@MockitoBean` for Spring-managed collaborators in `@WebMvcTest`; this repo does not use `@MockBean`.
- Controller tests mock the service, so they verify serialization, status codes and validation, not business rules. Business rules belong in `InventoryItemImpTest`.
- Do not mock `InventoryItemMapper` in `InventoryItemMapperTest`; the point is to test the real MapStruct-generated mapping.

## Test data and fixtures

- Build DTOs inline: `new InventoryItemRequest("Steel Bolt M10", "...", 1L, new BigDecimal("2.50"), ...)`.
- Build entities with `InventoryItem.builder()...build()`; set an `id` only when persistence is not involved.
- Keep one standard fixture shape across classes; `createStandardResponse()` and friends keep tests short and comparable.
- Use `new BigDecimal("2.50")` and compare with `isEqualByComparingTo("2.50")`, not `isEqualTo`, to avoid scale and precision flakiness.
- Boundary fixtures: maximum field lengths (100-char name, 200-char description) and minimum decimals (`0.01`) in `InventoryItemMapperTest`.
- No shared JSON fixture files for tests; payloads are serialized from records through the injected `ObjectMapper`.

## Coverage gates and exclusions

JaCoCo is configured in `pom.xml`:

- `prepare-agent` instruments tests; `report` is bound to the `test` phase (`target/site/jacoco/index.html`); `check` runs at `verify`.
- Bundle rules: line coverage ≥ 0.80, branch coverage ≥ 0.70, missed classes = 0.
- Package rule: line coverage ≥ 0.70, excluding `com.elara.app.inventory_service.config.*` and `InventoryServiceApplication`.
- Global exclusions: `**/InventoryServiceApplication.class`, `**/config/**`, `**/dto/**`. Detection is path-based; `@Generated` annotations are not an exclusion mechanism.
- DTOs and config are exempt from the gate, but `controller`, `service`, `repository`, `mapper` and `exceptions` code is not.

## Exact commands

```bash
sh ./mvnw test                                             # all tests
sh ./mvnw test -Dtest=InventoryItemControllerTest          # one class
sh ./mvnw test -Dtest=InventoryItemImpTest#save_withValidRequest_createsAndReturnsResponse
sh ./mvnw -B clean verify -Dspring.profiles.active=test    # tests + coverage check
```

The Maven wrapper is mode 644; always invoke it as `sh ./mvnw` and never rely on an executable invocation. Open the coverage report at `target/site/jacoco/index.html`.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Mocks are null | Missing Mockito extension or wrong annotations | Use `@ExtendWith(MockitoExtension.class)` with `@Mock`/`@InjectMocks`, or `@MockitoBean` inside `@WebMvcTest` |
| `UnnecessaryStubbingException` | A stub does not apply to the path under test | Remove unused stubs or move them into the relevant test |
| Test bleeds state into another test | Mocks or shared fixtures keep state | Reset mocks in `@AfterEach`; build fixtures per test |
| BigDecimal assertion fails on equal values | `isEqualTo` compares scale | Use `assertThat(value).isEqualByComparingTo("2.50")` |
| Repository test fails on duplicate name | `name` is `unique = true` | Persist a distinct name per test |
| Controller test returns 500 instead of 4xx | Service or message stub missing for the error path | Stub `service` and, when asserting messages, `messageService` |
| Spring context fails to start | Non-test integration (config/vault/eureka) was not disabled | Add `@ActiveProfiles("test")` to the test class |
| Coverage check fails at `verify` | New branches are uncovered | Open `target/site/jacoco/index.html` and cover the missed lines |
| Sort test passes but the live API fails | The web slice mocks the service | Test sort behavior against the running stack or a repository test |

## Checklist for a new test

1. Pick the layer: rule → `InventoryItemImpTest`, HTTP → `InventoryItemControllerTest`, query → `InventoryItemRepositoryTest`, mapping → `InventoryItemMapperTest`.
2. Add a `@Nested` class only when a new operation group appears.
3. Write the Given-When-Then body with fresh fixtures and no shared state.
4. Name it `method_condition_expectedResult` and add `@DisplayName`.
5. Run the focused class first, then `sh ./mvnw -B clean verify -Dspring.profiles.active=test`.
6. If the change touches the API contract, update the OpenAPI annotations/examples and the README table in the same change.

## Related documentation

- [README.md](README.md) — API contract and the coverage summary.
- [AGENTS.md](AGENTS.md) — commands, invariants and traps.
