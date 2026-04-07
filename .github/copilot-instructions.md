# Copilot Instructions - Inventory Service

**📖 Reference:** See [`AGENTS.md`](../AGENTS.md) for comprehensive guidelines on code style, architecture, testing, and error handling.

This file provides quick reference for the most critical commands and patterns.

## Quick Start

### Build & Test
```bash
# Build the project
./mvnw clean install

# Build without running tests
./mvnw clean install -DskipTests

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=InventoryItemControllerTest

# Run a specific test method
./mvnw test -Dtest=InventoryItemServiceImpTest#save_withUniqueName_createsAndReturnsResponse

# Run tests with coverage report (generates target/site/jacoco/index.html)
./mvnw clean verify

# Run the application
./mvnw spring-boot:run
```

## Project Structure

**Stack:** Spring Boot 3.5.6, Java 21, Maven, PostgreSQL/H2

**Package:** `com.elara.app.inventory_service`

```
src/main/java/com/elara/app/inventory_service/
├── config/              # Configuration (AppConfig, OpenApiConfig, GlobalExceptionHandler)
├── controller/          # REST controllers
├── dto/                 # Data Transfer Objects
│   ├── request/        # Request DTOs (use records)
│   ├── response/       # Response DTOs (use records)
│   └── update/         # Update DTOs (use records)
├── exceptions/         # Custom exceptions (ResourceNotFoundException, ResourceConflictException, etc.)
├── mapper/            # MapStruct mappers (@Mapper(componentModel = "spring"))
├── model/             # JPA entities
├── repository/        # Spring Data JPA repositories
├── service/           
│   ├── imp/          # Service implementations (@Service, @Transactional)
│   └── interfaces/   # Service interfaces
└── utils/            # Utilities (MessageService, ErrorCode, ApplicationContextHolder)
```

## Key Architecture Patterns

### Code Quality & Coverage
- **Line coverage:** 80% minimum (enforced by JaCoCo)
- **Branch coverage:** 70% minimum
- **Excluded from coverage:** DTOs, config classes, main application class

### DTOs Always Use Records
```java
public record InventoryItemRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 200) String description,
    @NotNull @Positive Long baseUnitOfMeasureId
) {}
```

### Entities Use Lombok & JPA
```java
@Entity(name = "inventory_item")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;
    
    @NotBlank @Size(max = 100)
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;
}
```

### Services Follow Pattern
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityImp implements EntityService {
    private static final String ENTITY_NAME = "Entity";
    private static final String NOMENCLATURE = ENTITY_NAME + "-service";
    
    private final EntityMapper mapper;
    private final EntityRepository repository;
    
    @Override
    @Transactional  // Always on methods that modify data
    public EntityResponse save(EntityRequest request) {
        final String methodNomenclature = NOMENCLATURE + "-save";
        log.info("[{}] Attempting to create {}: {}", methodNomenclature, ENTITY_NAME, request);
        
        // Validation first
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ResourceConflictException("name", request.name());
        }
        
        Entity entity = mapper.toEntity(request);
        Entity saved = repository.save(entity);
        return mapper.toResponse(saved);
    }
}
```

## Critical Conventions

### Naming
- **Classes:** Controllers: `{Entity}Controller` | Services: `{Entity}Service`/`{Entity}Imp` | DTOs: `{Entity}Request/Response/Update`
- **Constants:** `UPPER_SNAKE_CASE`
- **Variables:** `camelCase`
- **Database columns:** `snake_case` (Java fields are camelCase)

### Logging with Nomenclature
```java
final String methodNomenclature = NOMENCLATURE + "-methodName";
log.info("[{}] Attempting to create {}: {}", methodNomenclature, ENTITY_NAME, request);
log.error("[{}] Unexpected error: {}", methodNomenclature, e.getMessage(), e);
```

### Error Codes (Standard)
| Code | HTTP | Usage |
|------|------|-------|
| 1001 | 500 | SERVICE_UNAVAILABLE |
| 1002 | 400 | INVALID_DATA |
| 1003 | 409 | RESOURCE_CONFLICT (duplicates) |
| 1004 | 404 | RESOURCE_NOT_FOUND |
| 1005 | 500 | DATABASE_ERROR |
| 1006 | 500 | UNEXPECTED_ERROR |

### Exception Throwing
```java
// Not found
throw new ResourceNotFoundException(ENTITY_NAME, "id", id.toString());

// Duplicate/conflict
throw new ResourceConflictException("name", request.name());

// Invalid data
throw new InvalidDataException("Invalid format");
```

## Testing Patterns

**Test Naming:** `method_condition_expectedResult`

**Controller Tests:** `@WebMvcTest` + `MockMvc`
```java
@WebMvcTest(EntityController.class)
class EntityControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private EntityService service;
    
    @AfterEach
    void tearDown() {
        reset(service);
    }
}
```

**Service Tests:** `@ExtendWith(MockitoExtension.class)` with Given-When-Then
```java
@ExtendWith(MockitoExtension.class)
class EntityServiceTest {
    @Mock private EntityRepository repository;
    @InjectMocks private EntityImp service;
    
    @AfterEach
    void tearDown() {
        reset(repository);
    }
}
```

## Integration Points

**Spring Cloud Components:**
- Eureka Client (service discovery)
- Config Server (centralized configuration)
- Vault (secrets management)
- RabbitMQ (event streaming via spring-cloud-bus-amqp)

**OpenAPI/Swagger:** All endpoints must have `@Operation` with summary/description and `@ApiResponses` for all HTTP codes.

## Critical Rules - DO NOT SKIP

1. ✅ Always use `@Transactional` on service methods that modify data
2. ✅ Never expose entities directly—always use DTOs
3. ✅ Log at entry/exit of service methods with nomenclature pattern
4. ✅ Validate external IDs before using them
5. ✅ Use `@Size`, `@Positive`, `@NotNull`, `@NotBlank` for validation
6. ✅ Use `BigDecimal` for money/quantities (never `float` or `double`)
7. ✅ Database columns must be snake_case; Java fields are camelCase
8. ✅ IDs use `@Positive` validation—never accept 0 or negative
9. ✅ Use `@AfterEach` with `reset()` in tests for mock isolation
10. ✅ MapStruct mappers use `componentModel = "spring"`

## Imports Organization

```java
// 1. Java standard library
import java.math.BigDecimal;

// 2. Jakarta EE
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

// 3. Spring Framework
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

// 4. Third-party (Lombok, MapStruct, etc.)
import lombok.*;
import lombok.extern.slf4j.Slf4j;

// 5. Internal packages
import com.elara.app.inventory_service.dto.*;
```

---

**Last Updated:** April 6, 2026  
**For comprehensive guidelines, refer to:** [`AGENTS.md`](../AGENTS.md)
