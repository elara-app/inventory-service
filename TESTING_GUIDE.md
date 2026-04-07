# Testing Guide - Inventory Service

Comprehensive testing practices and patterns for the Inventory Service microservice. This guide covers testing strategy, test organization, and best practices with real examples from the codebase.

## Table of Contents

1. [Testing Philosophy](#testing-philosophy)
2. [Test Organization](#test-organization)
3. [Test Types](#test-types)
4. [Common Patterns](#common-patterns)
5. [Mocking Strategies](#mocking-strategies)
6. [Test Fixtures & Data](#test-fixtures--data)
7. [Best Practices](#best-practices)
8. [Coverage Requirements](#coverage-requirements)
9. [Troubleshooting](#troubleshooting)

---

## Testing Philosophy

### Core Principles

1. **Test in Isolation** - Each test is independent; no shared state between tests
2. **Arrange-Act-Assert** - Organize tests with Given-When-Then structure
3. **Single Responsibility** - One test, one behavior verification
4. **Meaningful Names** - Test names describe what is being tested and expected
5. **Fast Feedback** - Tests run quickly to encourage frequent execution
6. **No Implementation Leakage** - Tests verify behavior, not internal details

### Coverage Strategy

The project enforces strict coverage metrics via JaCoCo:
- **Line Coverage:** 80% minimum (enforced)
- **Branch Coverage:** 70% minimum (enforced)
- **Class Coverage:** 0 uncovered classes (enforced)
- **Exclusions:** DTOs, config classes, `InventoryServiceApplication`

Test different aspects:
- **Happy path:** Normal, expected behavior
- **Edge cases:** Boundary values, null handling, empty collections
- **Error paths:** Exceptions, validation failures, conflicts
- **Integration:** Component interactions, database operations

---

## Test Organization

### Test Naming Convention

Follow the pattern: `method_condition_expectedResult`

**Examples:**
- `save_withUniqueName_createsAndReturnsResponse`
- `findById_withNonExistentId_throwsResourceNotFoundException`
- `update_withSameNameButDifferentCase_updatesSuccessfully`
- `toEntity_withNullDescription_handlesCorrectly`

### Test Structure

All tests use `@DisplayName` for human-readable names in reports:

```java
@DisplayName("InventoryItemImp Service Tests")
class InventoryItemImpTest {
    // ... tests here
}
```

Use `@Nested` classes to logically group related tests:

```java
@Nested
@DisplayName("Save Operations")
class SaveOperations {
    // Related save tests here
}
```

### Test Directories

```
src/test/java/com/elara/app/inventory_service/
├── controller/                # REST endpoint tests
├── service/imp/              # Service implementation tests
├── repository/               # Data access tests
├── mapper/                   # DTO mapping tests
├── exceptions/               # Exception class tests
├── utils/                    # Utility class tests
└── InventoryServiceApplicationTests.java  # Integration tests
```

---

## Test Types

### 1. Unit Tests - Service Layer

Test business logic in isolation using mocks.

**Framework:** `@ExtendWith(MockitoExtension.class)`  
**Mocks:** Repositories, other services, external dependencies  
**Scope:** Pure method logic, no Spring context

**Example:**
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryItemImp Service Tests")
class InventoryItemImpTest {
    
    @Mock
    private InventoryItemRepository repository;
    
    @Mock
    private InventoryItemMapper mapper;
    
    @InjectMocks
    private InventoryItemImp service;
    
    @AfterEach
    void tearDown() {
        reset(repository, mapper);
    }
    
    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {
        
        @Test
        @DisplayName("save: with unique name, creates and returns response")
        void save_withUniqueName_createsAndReturnsResponse() {
            // Given
            InventoryItemRequest request = createStandardRequest();
            InventoryItem entity = createStandardEntity();
            InventoryItemResponse expectedResponse = createStandardResponse();
            
            when(repository.existsByNameIgnoreCase(request.name()))
                .thenReturn(false);
            when(mapper.toEntity(request))
                .thenReturn(entity);
            when(repository.save(entity))
                .thenReturn(entity);
            when(mapper.toResponse(entity))
                .thenReturn(expectedResponse);
            
            // When
            InventoryItemResponse result = service.save(request);
            
            // Then
            assertThat(result)
                .isNotNull()
                .isEqualTo(expectedResponse);
            verify(repository).existsByNameIgnoreCase(request.name());
            verify(repository).save(entity);
        }
        
        @Test
        @DisplayName("save: with duplicate name, throws ResourceConflictException")
        void save_withDuplicateName_throwsResourceConflictException() {
            // Given
            InventoryItemRequest request = createStandardRequest();
            when(repository.existsByNameIgnoreCase(request.name()))
                .thenReturn(true);
            
            // When & Then
            assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists");
            
            verify(repository).existsByNameIgnoreCase(request.name());
            verify(repository, never()).save(any());
        }
    }
}
```

### 2. Integration Tests - Repository Layer

Test JPA queries and database operations with real schema.

**Framework:** `@DataJpaTest`  
**Database:** H2 in-memory (test profile)  
**Scope:** Query correctness, constraints, relationships

**Example:**
```java
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("InventoryItemRepository")
class InventoryItemRepositoryTest {
    
    @Autowired
    private InventoryItemRepository repository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Nested
    @DisplayName("FindByNameIgnoreCase")
    class FindByNameIgnoreCaseTests {
        
        @Test
        @DisplayName("findByNameIgnoreCase: with existing name (different case), returns item")
        void findByNameIgnoreCase_withExistingNameDifferentCase_returnsItem() {
            // Given
            InventoryItem item = createAndPersistItem(
                "Steel Bolt M10",
                "Description",
                1L,
                new BigDecimal("2.50"),
                new BigDecimal("100.00"),
                new BigDecimal("500.00")
            );
            
            // When
            Optional<InventoryItem> result = repository.findByNameIgnoreCase("steel bolt m10");
            
            // Then
            assertThat(result)
                .isPresent()
                .contains(item);
        }
        
        @Test
        @DisplayName("findByNameIgnoreCase: with non-existent name, returns empty")
        void findByNameIgnoreCase_withNonExistentName_returnsEmpty() {
            // When
            Optional<InventoryItem> result = repository.findByNameIgnoreCase("NonExistent");
            
            // Then
            assertThat(result).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("ExistsByNameIgnoreCase")
    class ExistsByNameIgnoreCaseTests {
        
        @Test
        @DisplayName("existsByNameIgnoreCase: with existing item, returns true")
        void existsByNameIgnoreCase_withExistingItem_returnsTrue() {
            // Given
            createAndPersistItem("Copper Wire", "Electrical wire", 2L,
                new BigDecimal("5.75"), new BigDecimal("200.00"), new BigDecimal("1000.00"));
            
            // When
            boolean exists = repository.existsByNameIgnoreCase("copper wire");
            
            // Then
            assertThat(exists).isTrue();
        }
        
        @Test
        @DisplayName("existsByNameIgnoreCase: with non-existent item, returns false")
        void existsByNameIgnoreCase_withNonExistentItem_returnsFalse() {
            // When
            boolean exists = repository.existsByNameIgnoreCase("NonExistent");
            
            // Then
            assertThat(exists).isFalse();
        }
    }
}
```

### 3. Controller Tests - REST API Layer

Test HTTP endpoints, status codes, and response formats.

**Framework:** `@WebMvcTest`  
**Mocks:** Services, dependencies  
**Tools:** `MockMvc` for HTTP simulation  
**Scope:** Request handling, status codes, error responses

**Example:**
```java
@WebMvcTest(InventoryItemController.class)
@DisplayName("InventoryItemController REST API Tests")
class InventoryItemControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private InventoryItemService service;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @AfterEach
    void tearDown() {
        reset(service);
    }
    
    @Nested
    @DisplayName("POST /item - Create")
    class CreateOperations {
        
        @Test
        @DisplayName("create: with valid request, returns 201 Created")
        void create_withValidRequest_returns201Created() throws Exception {
            // Given
            InventoryItemRequest request = createStandardRequest();
            InventoryItemResponse response = createStandardResponse();
            
            when(service.save(request))
                .thenReturn(response);
            
            // When & Then
            mockMvc.perform(post("/item/")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id()))
                .andExpect(jsonPath("$.name").value(response.name()));
            
            verify(service).save(request);
        }
        
        @Test
        @DisplayName("create: with invalid request (blank name), returns 400 Bad Request")
        void create_withBlankName_returns400BadRequest() throws Exception {
            // Given
            InventoryItemRequest request = new InventoryItemRequest(
                "",  // blank name
                "Description",
                1L,
                new BigDecimal("2.50"),
                new BigDecimal("100.00"),
                new BigDecimal("500.00")
            );
            
            // When & Then
            mockMvc.perform(post("/item/")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
            
            verify(service, never()).save(any());
        }
        
        @Test
        @DisplayName("create: with duplicate name, returns 409 Conflict")
        void create_withDuplicateName_returns409Conflict() throws Exception {
            // Given
            InventoryItemRequest request = createStandardRequest();
            
            when(service.save(request))
                .thenThrow(new ResourceConflictException("name", request.name()));
            
            // When & Then
            mockMvc.perform(post("/item/")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1003));
            
            verify(service).save(request);
        }
    }
    
    @Nested
    @DisplayName("GET /item/{id}")
    class GetByIdOperations {
        
        @Test
        @DisplayName("getById: with existing ID, returns 200 OK with item")
        void getById_withExistingId_returns200Ok() throws Exception {
            // Given
            Long itemId = 1L;
            InventoryItemResponse response = createStandardResponse();
            
            when(service.findById(itemId))
                .thenReturn(response);
            
            // When & Then
            mockMvc.perform(get("/item/{id}", itemId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.name").value(response.name()));
            
            verify(service).findById(itemId);
        }
        
        @Test
        @DisplayName("getById: with non-existent ID, returns 404 Not Found")
        void getById_withNonExistentId_returns404NotFound() throws Exception {
            // Given
            Long itemId = 999L;
            
            when(service.findById(itemId))
                .thenThrow(new ResourceNotFoundException("InventoryItem", "id", itemId.toString()));
            
            // When & Then
            mockMvc.perform(get("/item/{id}", itemId)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1004));
            
            verify(service).findById(itemId);
        }
    }
}
```

### 4. Mapper Tests - DTO Transformation

Test data transformation between DTOs and entities.

**Framework:** `@SpringBootTest` with `@ActiveProfiles("test")`  
**Scope:** Correct field mapping, type conversion, null handling

**Example:**
```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("InventoryItemMapper")
class InventoryItemMapperTest {
    
    @Autowired
    private InventoryItemMapper mapper;
    
    @Nested
    @DisplayName("ToEntity - Request to Entity Mapping")
    class ToEntityTests {
        
        @Test
        @DisplayName("toEntity: with valid request, maps all fields correctly")
        void toEntity_withValidRequest_mapsAllFieldsCorrectly() {
            // Given
            InventoryItemRequest request = new InventoryItemRequest(
                "Steel Bolt M10",
                "High-strength steel bolt",
                1L,
                new BigDecimal("2.50"),
                new BigDecimal("100.00"),
                new BigDecimal("500.00")
            );
            
            // When
            InventoryItem entity = mapper.toEntity(request);
            
            // Then
            assertThat(entity)
                .isNotNull()
                .extracting("name", "description", "baseUnitOfMeasureId")
                .containsExactly("Steel Bolt M10", "High-strength steel bolt", 1L);
            assertThat(entity.getStandardCost()).isEqualByComparingTo("2.50");
        }
        
        @Test
        @DisplayName("toEntity: with null description, handles correctly")
        void toEntity_withNullDescription_handlesCorrectly() {
            // Given
            InventoryItemRequest request = new InventoryItemRequest(
                "Item Without Description",
                null,
                1L,
                new BigDecimal("10.00"),
                new BigDecimal("50.00"),
                new BigDecimal("200.00")
            );
            
            // When
            InventoryItem entity = mapper.toEntity(request);
            
            // Then
            assertThat(entity)
                .isNotNull()
                .hasFieldOrPropertyWithValue("description", null);
        }
        
        @Test
        @DisplayName("toEntity: with BigDecimal precision, preserves decimal places")
        void toEntity_withBigDecimalPrecision_preservesDecimalPlaces() {
            // Given
            InventoryItemRequest request = new InventoryItemRequest(
                "Precision Item",
                "Test",
                1L,
                new BigDecimal("99.99"),
                new BigDecimal("123.45"),
                new BigDecimal("678.90")
            );
            
            // When
            InventoryItem entity = mapper.toEntity(request);
            
            // Then
            assertThat(entity.getStandardCost()).isEqualByComparingTo("99.99");
            assertThat(entity.getUnitPerPurchaseUom()).isEqualByComparingTo("123.45");
            assertThat(entity.getReorderPointQuantity()).isEqualByComparingTo("678.90");
        }
    }
}
```

### 5. Exception Tests

Test custom exceptions handle edge cases correctly.

**Framework:** `@Test` with assertions  
**Scope:** Exception construction, message formatting, field initialization

```java
@DisplayName("Custom Exceptions")
class CustomExceptionsTest {
    
    @Nested
    @DisplayName("ResourceConflictException")
    class ResourceConflictExceptionTests {
        
        @Test
        @DisplayName("constructor: initializes with field name and value")
        void constructor_initializesWithFieldNameAndValue() {
            // When
            ResourceConflictException exception = new ResourceConflictException("email", "test@example.com");
            
            // Then
            assertThat(exception)
                .hasMessageContaining("email")
                .hasMessageContaining("test@example.com");
        }
    }
}
```

---

## Common Patterns

### Given-When-Then Structure

All tests follow this structure:

```java
@Test
@DisplayName("service: operation, expected result")
void testName() {
    // Given - Setup test data and mocks
    User user = new User("John", "john@example.com");
    when(repository.findById(1L)).thenReturn(Optional.of(user));
    
    // When - Execute the code under test
    User result = service.getUserById(1L);
    
    // Then - Verify the outcome
    assertThat(result).isEqualTo(user);
    verify(repository).findById(1L);
}
```

### Assertion Helpers

Use AssertJ for expressive assertions:

```java
// Null checks
assertThat(value).isNull();
assertThat(value).isNotNull();

// Equality
assertThat(value).isEqualTo(expected);
assertThat(value).isNotEqualTo(unexpected);

// Collections
assertThat(list).isEmpty();
assertThat(list).hasSize(3);
assertThat(list).contains(item1, item2);
assertThat(list).doesNotContain(item3);

// BigDecimal (use comparison, not equality)
assertThat(amount).isEqualByComparingTo("100.00");
assertThat(amount).isGreaterThan(new BigDecimal("50.00"));

// Exceptions
assertThatThrownBy(() -> service.save(request))
    .isInstanceOf(ResourceConflictException.class)
    .hasMessageContaining("already exists");

// Objects with multiple fields
assertThat(response)
    .isNotNull()
    .extracting("id", "name", "email")
    .containsExactly(1L, "John", "john@example.com");
```

### Mockito Patterns

**Setup mocks:**
```java
// Return value
when(repository.save(any(Entity.class)))
    .thenReturn(savedEntity);

// Throw exception
when(repository.findById(999L))
    .thenThrow(new ResourceNotFoundException("Entity", "id", "999"));

// Multiple calls
when(repository.existsByName(anyString()))
    .thenReturn(false)
    .thenReturn(true);
```

**Verify interactions:**
```java
// Called once with specific argument
verify(repository).save(entity);

// Called exactly N times
verify(repository, times(2)).findAll();

// Never called
verify(repository, never()).delete(any());

// Verify call order
InOrder inOrder = inOrder(repository, service);
inOrder.verify(service).validate(request);
inOrder.verify(repository).save(entity);
```

**Argument Matchers:**
```java
any()              // Any value
any(String.class)  // Any string
anyLong()          // Any long
argThat(x -> x > 0)  // Custom matcher
eq("exact")        // Exact match
```

---

## Mocking Strategies

### Service Layer Mocking

Mock external dependencies but test service logic:

```java
@ExtendWith(MockitoExtension.class)
class InventoryItemImpTest {
    
    @Mock private InventoryItemRepository repository;
    @Mock private InventoryItemMapper mapper;
    @Mock private MessageService messageService;
    @Mock private UomServiceClient uomServiceClient;
    
    @InjectMocks
    private InventoryItemImp service;
    
    @AfterEach
    void tearDown() {
        // Reset ALL mocks to ensure test isolation
        reset(repository, mapper, messageService, uomServiceClient);
    }
    
    @Test
    void save_withValidRequest_callsValidationThenSave() {
        // Mock the flow
        InventoryItemRequest request = new InventoryItemRequest(...);
        
        // When repository is asked about duplicates, say no
        when(repository.existsByNameIgnoreCase(request.name()))
            .thenReturn(false);
        
        // When mapper is asked to convert, return an entity
        InventoryItem entity = new InventoryItem(...);
        when(mapper.toEntity(request))
            .thenReturn(entity);
        
        // When repository saves, return the saved entity
        when(repository.save(entity))
            .thenReturn(entity);
        
        // Test executes normally
        service.save(request);
        
        // Verify the sequence of calls
        InOrder inOrder = inOrder(repository, mapper);
        inOrder.verify(repository).existsByNameIgnoreCase(request.name());
        inOrder.verify(mapper).toEntity(request);
        inOrder.verify(repository).save(entity);
    }
}
```

### Controller Layer Mocking

Mock services, test HTTP layer:

```java
@WebMvcTest(InventoryItemController.class)
class InventoryItemControllerTest {
    
    @Autowired private MockMvc mockMvc;
    @MockitoBean private InventoryItemService service;
    
    @Test
    void create_withValidRequest_returns201() throws Exception {
        // Mock service response
        InventoryItemResponse response = new InventoryItemResponse(1L, "Item", null, 1L, ...);
        when(service.save(any())).thenReturn(response);
        
        // Execute HTTP request
        mockMvc.perform(post("/item/")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{...}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
        
        verify(service).save(any());
    }
}
```

### Avoiding Over-Mocking

❌ **Bad** - Mocks internal behavior:
```java
// Don't mock what you're testing
when(repository.findAll()).thenReturn(pageOfItems);
Page<Item> result = service.findAll(pageable);
```

✅ **Good** - Mock only external dependencies:
```java
// Mock the repository (external dependency)
when(repository.findAll(pageable)).thenReturn(pageOfItems);
// Test service's transformation logic
Page<ItemResponse> result = service.findAll(pageable);
```

---

## Test Fixtures & Data

### Test Data Builders

Create reusable test data helpers:

```java
private InventoryItemRequest createStandardRequest() {
    return new InventoryItemRequest(
        "Steel Bolt M10",
        "High-strength steel bolt, M10 thread",
        1L,
        new BigDecimal("2.50"),
        new BigDecimal("10.00"),
        new BigDecimal("50.00")
    );
}

private InventoryItemRequest createRequestWithName(String name) {
    return new InventoryItemRequest(
        name,
        "Description",
        1L,
        new BigDecimal("2.50"),
        new BigDecimal("100.00"),
        new BigDecimal("500.00")
    );
}

private InventoryItem createStandardEntity() {
    return InventoryItem.builder()
        .id(1L)
        .name("Steel Bolt M10")
        .description("High-strength steel bolt")
        .baseUnitOfMeasureId(1L)
        .standardCost(new BigDecimal("2.50"))
        .unitPerPurchaseUom(new BigDecimal("100.00"))
        .reorderPointQuantity(new BigDecimal("500.00"))
        .build();
}

private InventoryItemResponse createStandardResponse() {
    return new InventoryItemResponse(
        1L,
        "Steel Bolt M10",
        "High-strength steel bolt",
        1L,
        new BigDecimal("2.50"),
        new BigDecimal("100.00"),
        new BigDecimal("500.00")
    );
}
```

### Database Setup (Repository Tests)

```java
@DataJpaTest
class InventoryItemRepositoryTest {
    
    @Autowired private TestEntityManager entityManager;
    @Autowired private InventoryItemRepository repository;
    
    private InventoryItem createAndPersistItem(String name, String description, Long baseUomId,
                                               BigDecimal standardCost, BigDecimal unitPerPurchase,
                                               BigDecimal reorderPoint) {
        InventoryItem item = InventoryItem.builder()
            .name(name)
            .description(description)
            .baseUnitOfMeasureId(baseUomId)
            .standardCost(standardCost)
            .unitPerPurchaseUom(unitPerPurchase)
            .reorderPointQuantity(reorderPoint)
            .build();
        entityManager.persist(item);
        entityManager.flush();
        return item;
    }
}
```

### Test Profiles

Use `@ActiveProfiles("test")` to load test-specific configuration:

```java
// application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
logging.level.root=WARN
```

---

## Best Practices

### 1. Test Independence

Each test must be independent:

```java
❌ BAD - Depends on test execution order
class OrderTests {
    static List<Order> orders = new ArrayList<>();
    
    @Test void createOrder() { orders.add(new Order()); }
    @Test void findOrder() { Order o = orders.get(0); }  // Fails if run alone!
}

✅ GOOD - Each test stands alone
class OrderTests {
    @Test void createOrder() {
        Order order = repository.save(new Order());
        assertThat(order).isNotNull();
    }
    
    @Test void findOrder() {
        Order saved = repository.save(new Order());
        Order found = repository.findById(saved.getId());
        assertThat(found).isEqualTo(saved);
    }
}
```

### 2. Use @AfterEach for Mock Reset

Always reset mocks after each test:

```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock private Repository repository;
    @InjectMocks private Service service;
    
    @AfterEach
    void tearDown() {
        reset(repository);  // Reset mock state
    }
    
    @Test void test1() { /* ... */ }
    @Test void test2() { /* ... */ }  // Mocks are clean
}
```

### 3. Meaningful Test Names and Descriptions

```java
// Use @DisplayName for readable test reports
@Test
@DisplayName("save: with duplicate name (case-insensitive), throws ResourceConflictException")
void save_withDuplicateName_throwsResourceConflictException() {
    // ...
}
```

### 4. One Assertion per Test (or Related Assertions)

Prefer focused tests:

```java
❌ BAD - Tests multiple unrelated behaviors
@Test
void saveLongTest() {
    service.save(request);
    service.update(updated);
    service.delete(id);
    // Multiple assertions on unrelated operations
}

✅ GOOD - One logical behavior per test
@Test void save_withValidRequest_returnsSavedItem() { }
@Test void update_withValidRequest_updatesItem() { }
@Test void delete_withValidId_removesItem() { }
```

### 5. Avoid Testing Implementation Details

```java
❌ BAD - Tests how it works, not what it does
@Test
void save_callsMapperBeforeRepository() {
    InOrder inOrder = inOrder(mapper, repository);
    inOrder.verify(mapper).toEntity(request);
    inOrder.verify(repository).save(entity);
}

✅ GOOD - Tests the outcome
@Test
void save_withValidRequest_returnsResponse() {
    InventoryItemResponse result = service.save(request);
    assertThat(result).isNotNull().hasFieldOrPropertyWithValue("id", 1L);
}
```

### 6. Test Edge Cases and Boundaries

```java
@Nested
@DisplayName("Edge Cases")
class EdgeCases {
    
    @Test
    @DisplayName("toEntity: with maximum field lengths, handles correctly")
    void toEntity_withMaximumFieldLengths_handlesCorrectly() {
        String maxName = "A".repeat(100);      // Max 100 chars
        String maxDescription = "B".repeat(200);  // Max 200 chars
        
        InventoryItemRequest request = new InventoryItemRequest(
            maxName, maxDescription, 1L, ...
        );
        
        InventoryItem result = mapper.toEntity(request);
        assertThat(result.getName()).hasSize(100);
        assertThat(result.getDescription()).hasSize(200);
    }
    
    @Test
    @DisplayName("toResponse: with minimum BigDecimal values, handles correctly")
    void toResponse_withMinimumBigDecimalValues_handlesCorrectly() {
        InventoryItem entity = InventoryItem.builder()
            .standardCost(new BigDecimal("0.01"))  // Minimum
            .build();
        
        InventoryItemResponse result = mapper.toResponse(entity);
        assertThat(result.getStandardCost()).isEqualByComparingTo("0.01");
    }
}
```

### 7. Use Nested Classes for Organization

```java
@DisplayName("InventoryItemService")
class InventoryItemServiceTest {
    
    @Nested
    @DisplayName("Save Operations")
    class SaveTests { }
    
    @Nested
    @DisplayName("Update Operations")
    class UpdateTests { }
    
    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests { }
    
    @Nested
    @DisplayName("Error Cases")
    class ErrorCases { }
}
```

---

## Coverage Requirements

### Enforced by JaCoCo

```bash
# Generate coverage report
./mvnw clean verify

# Report location
target/site/jacoco/index.html
```

### Minimum Standards

| Metric | Minimum | Type |
|--------|---------|------|
| Line Coverage | 80% | Required (BUNDLE) |
| Branch Coverage | 70% | Required (BUNDLE) |
| Uncovered Classes | 0 | Required (BUNDLE) |
| Package Line Coverage | 70% | Required |

### Coverage Exclusions

The following are excluded from coverage requirements:
- `**/*.dto/**` - All DTO classes (records)
- `**/config/**` - Configuration classes
- `InventoryServiceApplication` - Main application class

Add `@jakarta.annotation.Generated` to exclude generated code:

```java
@Generated  // Excluded from coverage
public class GeneratedClass {
    // ...
}
```

### Improving Coverage

**Identify gaps:**
```bash
# View coverage report
open target/site/jacoco/index.html
```

**Add missing tests for:**
- Error paths (exceptions, validation failures)
- Boundary conditions (null, empty, max values)
- Alternative branches (if/else conditions)
- Integration scenarios

---

## Troubleshooting

### Issue: Mock Not Injected

**Problem:** `@InjectMocks` field is null

**Solution:** Use `@ExtendWith(MockitoExtension.class)` on test class:
```java
@ExtendWith(MockitoExtension.class)  // Required for @InjectMocks
class ServiceTest {
    @Mock private Repository repository;
    @InjectMocks private Service service;  // Now service.repository is injected
}
```

### Issue: Test Isolation Failure

**Problem:** One test's data affects another test

**Solution:** Use `@AfterEach` with `reset()`:
```java
@AfterEach
void tearDown() {
    reset(repository, mapper, messageService);  // Reset ALL mocks
}
```

### Issue: Flaky BigDecimal Assertions

**Problem:** `isEqualTo()` fails due to scale differences

**Solution:** Use `isEqualByComparingTo()`:
```java
✅ CORRECT - Compares numeric value
assertThat(new BigDecimal("10.00"))
    .isEqualByComparingTo(new BigDecimal("10"));

❌ WRONG - Fails because scale differs
assertThat(new BigDecimal("10.00"))
    .isEqualTo(new BigDecimal("10"));
```

### Issue: Pagination Test Issues

**Problem:** PageImpl constructor confusion

**Solution:** Use correct constructor:
```java
List<Item> items = Arrays.asList(item1, item2);
Pageable pageable = PageRequest.of(0, 20);
Page<Item> page = new PageImpl<>(items, pageable, items.size());
```

### Issue: MockMvc Request/Response Format

**Problem:** JSON serialization/deserialization errors

**Solution:** Use ObjectMapper correctly:
```java
@Autowired
private ObjectMapper objectMapper;

// Serialize object to JSON
String json = objectMapper.writeValueAsString(request);

// Use in request
mockMvc.perform(post("/item/")
    .contentType(MediaType.APPLICATION_JSON)
    .content(json))
    ...

// Verify response
.andExpect(jsonPath("$.id").value(1L))
```

### Issue: Test Fails Locally but Passes in CI

**Problem:** Likely database profile or timing issue

**Solutions:**
- Ensure `@ActiveProfiles("test")` is set
- Add explicit waits if async operations exist
- Check database transaction isolation level
- Verify test data cleanup (use `@AfterEach` or `@DataJpaTest` auto-rollback)

### Issue: Coverage Report Not Generated

**Problem:** `target/site/jacoco/index.html` missing

**Solution:** Run with verify phase:
```bash
./mvnw clean verify
```

Not just `mvnw test` — you need the `verify` phase for JaCoCo reports.

---

## Quick Reference

### Running Tests

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=InventoryItemServiceTest

# Single test method
./mvnw test -Dtest=InventoryItemServiceTest#save_withUniqueName_createsAndReturnsResponse

# With coverage report
./mvnw clean verify

# Skip tests
./mvnw clean install -DskipTests
```

### Test Annotations

| Annotation | Usage |
|------------|-------|
| `@Test` | Marks method as test |
| `@DisplayName("...")` | Readable name in reports |
| `@Nested` | Groups related tests |
| `@BeforeEach` | Runs before each test |
| `@AfterEach` | Runs after each test (cleanup) |
| `@ParameterizedTest` | Multiple test cases |
| `@ExtendWith(...)` | Extension (e.g., Mockito) |
| `@Mock` | Creates mock object |
| `@InjectMocks` | Injects mocks into object |
| `@WebMvcTest(...)` | Load minimal Spring context for controllers |
| `@DataJpaTest` | Load Spring context for repositories |
| `@SpringBootTest` | Full Spring application context |
| `@ActiveProfiles("test")` | Use test configuration |

---

**Last Updated:** April 6, 2026  
**Version:** 1.0  
**Related:** See [`AGENTS.md`](AGENTS.md) for code style and architecture guidelines.
