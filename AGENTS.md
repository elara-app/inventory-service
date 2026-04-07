# Agent Guidelines - Inventory Service

This document provides essential information for AI coding agents working on this Spring Boot 3.5.6 microservice.

## Project Overview

- **Project**: Elara Inventory Service (Spring Boot 3.5.6, Java 21)
- **Build Tool**: Maven 3.x
- **Package**: `com.elara.app.inventory_service`
- **Architecture**: Spring Cloud microservice with Eureka, Config Server, Vault, and RabbitMQ-based Spring Cloud Bus for distributed config refresh

## Build, Test & Run Commands

### Build & Package
```bash
# Clean and build
./mvnw clean install

# Build without tests
./mvnw clean install -DskipTests

# Create executable JAR
./mvnw clean package
```

### Testing
```bash
# Run all tests
./mvnw test

# Run tests with coverage report
./mvnw clean verify

# Run specific test class
./mvnw test -Dtest=InventoryItemControllerTest

# Run specific test method
./mvnw test -Dtest=InventoryItemImpTest#save_withValidRequest_createsAndReturnsResponse

# Run tests with verbose output
./mvnw test -Dsurefire.useFile=false
```

### Running the Application
```bash
# Run with default profile
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Code Quality
```bash
# Generate JaCoCo coverage report (target/site/jacoco/index.html)
./mvnw verify

# Coverage requirements:
# - Line coverage: 80% minimum
# - Branch coverage: 70% minimum
# - No uncovered classes (excludes DTOs, config classes, and main application class)
```

## Code Style Guidelines

### Package Structure
```
com.elara.app.inventory_service/
├── config/                 # Configuration classes (AppConfig, OpenApiConfig, GlobalExceptionHandler)
├── controller/             # REST controllers (@RestController)
├── dto/                    # Data Transfer Objects
│   ├── request/           # Request DTOs (records)
│   ├── response/          # Response DTOs (records)
│   └── update/            # Update DTOs (records)
├── exceptions/            # Custom exception classes
├── mapper/                # MapStruct mappers
├── model/                 # JPA entities
├── repository/            # Spring Data JPA repositories
├── service/
│   ├── imp/              # Service implementations
│   └── interfaces/       # Service interfaces
└── utils/                # Utilities (MessageService, ErrorCode, ApplicationContextHolder)
```

### Imports Organization
```java
// 1. Java standard library
import java.math.BigDecimal;
import java.util.Objects;

// 2. Jakarta EE
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

// 3. Spring Framework
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

// 4. Third-party libraries (Lombok, MapStruct, etc.)
import lombok.*;
import lombok.extern.slf4j.Slf4j;

// 5. Internal packages (com.elara.app.*)
import com.elara.app.inventory_service.dto.*;
import com.elara.app.inventory_service.service.interfaces.*;
```

### Naming Conventions

**Classes:**
- Controllers: `{Entity}Controller` (e.g., `InventoryItemController`)
- Services: `{Entity}Service` interface, `{Entity}Imp` implementation
- Repositories: `{Entity}Repository`
- DTOs: `{Entity}Request`, `{Entity}Response`, `{Entity}Update`
- Entities: `{Entity}` (e.g., `InventoryItem`)
- Exceptions: `{Type}Exception` (e.g., `ResourceNotFoundException`)

**Variables & Constants:**
```java
// Constants: UPPER_SNAKE_CASE
private static final String ENTITY_NAME = "InventoryItem";
private static final String NOMENCLATURE = ENTITY_NAME + "-service";

// Variables: camelCase
private final InventoryItemService service;

// Method variables: camelCase with descriptive names
final String methodNomenclature = NOMENCLATURE + "-save";
```

**Methods:**
- CRUD: `save()`, `update()`, `deleteById()`, `findById()`, `findAll()`
- Queries: `findAllByName()`, `existsByNameIgnoreCase()`
- Checks: `isNameTaken()`, `verifyUomById()`

### DTOs - Use Records
```java
public record InventoryItemRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 200) String description,
    @NotNull @Positive Long baseUnitOfMeasureId,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) 
    @Digits(integer = 10, fraction = 2) BigDecimal standardCost
) {}
```

### Entities - JPA with Lombok
```java
@Entity(name = "inventory_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;
    
    @NotBlank @Size(max = 100)
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;
    
    // Always use snake_case for column names
}
```

### Controllers - REST API Pattern
```java
@RestController
@RequestMapping(value = "item/", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Entity Management", description = "...")
public class EntityController {
    private final EntityService service;
    private static final String ENTITY_NAME = "Entity";
    private static final String NOMENCLATURE = ENTITY_NAME + "-controller";
    
    // Section comments for clarity
    // ========================================
    // CREATE OPERATIONS
    // ========================================
    
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "...", description = "...")
    public ResponseEntity<EntityResponse> create(@Valid @RequestBody EntityRequest request) {
        final String methodNomenclature = NOMENCLATURE + "-create";
        log.info("[{}] Request to create {}: {}", methodNomenclature, ENTITY_NAME, request);
        EntityResponse response = service.save(request);
        log.info("[{}] {} created with id: {}", methodNomenclature, ENTITY_NAME, response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Services - Business Logic
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityImp implements EntityService {
    private static final String ENTITY_NAME = "Entity";
    private static final String NOMENCLATURE = ENTITY_NAME + "-service";
    
    private final EntityMapper mapper;
    private final EntityRepository repository;
    private final MessageService messageService;
    
    @Override
    @Transactional
    public EntityResponse save(EntityRequest request) {
        final String methodNomenclature = NOMENCLATURE + "-save";
        log.info("[{}] Attempting to create {} with name: {}", methodNomenclature, ENTITY_NAME, request.name());
        
        // Validation
        if (repository.existsByNameIgnoreCase(request.name())) {
            String message = messageService.getMessage("crud.already.exists", ENTITY_NAME, "name", request.name());
            throw new ResourceConflictException(message);
        }
        
        // Save and return
        Entity entity = mapper.toEntity(request);
        Entity saved = repository.save(entity);
        return mapper.toResponse(saved);
    }
}
```

### Validation Annotations
Use Jakarta validation on DTOs and entities:
- `@NotNull` - Field cannot be null
- `@NotBlank` - String cannot be null, empty, or whitespace
- `@Size(max = 100)` - String/collection size constraints
- `@Positive` - Number must be > 0
- `@DecimalMin(value = "0.0", inclusive = false)` - Decimal validation
- `@Digits(integer = 10, fraction = 2)` - Decimal precision

### Error Handling

**Standard Error Codes:**

| Code | Value | HTTP Status | Usage |
|------|-------|-------------|-------|
| 1001 | DATABASE_ERROR | 500 | Database operation failed |
| 1002 | INVALID_DATA | 400 | Validation errors |
| 1003 | RESOURCE_CONFLICT | 409 | Duplicate/conflict |
| 1004 | RESOURCE_NOT_FOUND | 404 | Resource doesn't exist |
| 1005 | SERVICE_UNAVAILABLE | 503 | External service down |
| 1006 | UNEXPECTED_ERROR | 500 | Unhandled exceptions |

**Exception Hierarchy:**

The project uses a `BaseException` hierarchy that automatically maps to error codes and HTTP status:

```java
// Base exception with error code mapping
public abstract class BaseException extends RuntimeException {
    private final int code;
    private final String value;
    
    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.value = errorCode.getValue();
    }
}

// Specific exception types
public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}

public class ResourceConflictException extends BaseException {
    public ResourceConflictException(String message) {
        super(ErrorCode.RESOURCE_CONFLICT, message);
    }
}

public class InvalidDataException extends BaseException {
    public InvalidDataException(String message) {
        super(ErrorCode.INVALID_DATA, message);
    }
}

public class DatabaseException extends BaseException {
    public DatabaseException(String message) {
        super(ErrorCode.DATABASE_ERROR, message);
    }
}

public class ServiceUnavailableException extends BaseException {
    public ServiceUnavailableException(String message) {
        super(ErrorCode.SERVICE_UNAVAILABLE, message);
    }
}
```

**GlobalExceptionHandler** automatically handles all `BaseException` subclasses and returns properly formatted error responses with correct HTTP status codes.

**Throwing Exceptions:**
```java
// Not found (404)
throw new ResourceNotFoundException(messageService.getMessage("crud.not.found", ENTITY_NAME, "id", id.toString()));

// Conflict/duplicate (409)
throw new ResourceConflictException(messageService.getMessage("crud.already.exists", ENTITY_NAME, "name", request.name()));

// Invalid data (400)
throw new InvalidDataException(messageService.getMessage("inventoryItem.invalid.data", request));

// Database error (500)
throw new DatabaseException(messageService.getMessage("global.error.database", e.getMessage()));

// Service unavailable (503)
throw new ServiceUnavailableException(messageService.getMessage("global.error.service.unavailable"));
```


### Logging Standards
```java
// Use SLF4J with nomenclature pattern
log.info("[{}] Attempting to create {} with name: {}", methodNomenclature, ENTITY_NAME, name);
log.warn("[{}] {}", methodNomenclature, errorMessage);
log.error("[{}] Unexpected error: {}", methodNomenclature, e.getMessage(), e);

// Include method nomenclature in all logs
final String methodNomenclature = NOMENCLATURE + "-methodName";
```

## Testing Standards

Reference the comprehensive [TESTING_GUIDE.md](TESTING_GUIDE.md) for detailed testing patterns.

**Quick Reference:**
- Controller tests: `@WebMvcTest` + `MockMvc`
- In `@WebMvcTest`, use `@MockitoBean` for mocked Spring-managed collaborators
- Service tests: `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`
- Repository tests: `@DataJpaTest` + `TestEntityManager`
- Mapper tests: `@SpringBootTest` + `@Autowired`
- Repository and integration tests commonly use `@ActiveProfiles("test")`
- Use Given-When-Then structure
- Test naming: `method_condition_expectedResult`
- Always use `@AfterEach` to reset mocks

## Documentation

**Swagger/OpenAPI:**
- All endpoints must have `@Operation` with summary and description
- Include `@ApiResponses` for all response codes (200/201, 400, 404, 409, 500)
- Use `@Parameter` for path/query parameters
- Reference schema examples: `@Schema(ref = "#/components/schemas/...")`
- `OpenApiConfig` loads example payloads from `src/main/resources/examples/*.json`; keep those files aligned with response schemas and controller examples

## Common Patterns

### Application Bootstrap & External Configuration

- `src/main/resources/application.yml` imports `configserver:http://localhost:8888` and starts with the `dev` profile by default.
- `src/main/resources/application-dev.yml` imports Vault secrets from `vault://secret/inventory-service/dev`.
- Keep `spring-cloud-starter-bus-amqp` when config refresh propagation across services is required; it is part of the distributed config workflow, not a local message queue feature.

### External Service Client Pattern

For calling other microservices (e.g., UOM Service), use `@LoadBalanced RestTemplate`:

**Interface:**
```java
public interface UomServiceClient {
    void verifyUomById(Long id);
}
```

**Implementation:**
```java
@Slf4j
@Service
public class UomServiceClientImp implements UomServiceClient {
    private static final String ENTITY_NAME = "Uom";
    private static final String NOMENCLATURE = ENTITY_NAME + "-service";
    
    @Value("${uom.service.name:unit-of-measure-service}")
    private String uomServiceName;
    
    private final RestTemplate restTemplate;
    private final MessageService messageService;
    
    public void verifyUomById(Long id) {
        final String methodNomenclature = NOMENCLATURE + "-existsById";
        final String baseUrl = "http://" + uomServiceName;
        try {
            URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .pathSegment(String.valueOf(id))
                .build()
                .toUri();
            
            log.info("[{}] Searching {} with id: {}", methodNomenclature, ENTITY_NAME, id);
            ResponseEntity<UomResponse> response = restTemplate.getForEntity(uri, UomResponse.class);
            log.info("[{}] Found: {}", methodNomenclature, response.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            String msg = messageService.getMessage("crud.not.found", ENTITY_NAME, "id", id.toString());
            log.warn("[{}] {}", methodNomenclature, msg);
            throw new ResourceNotFoundException(msg);
        }
    }
}
```

**Key Points:**
- Use `@LoadBalanced` RestTemplate for Eureka service discovery (configured in `AppConfig`)
- Inject service name via `@Value` from `application.yml`
- Use `UriComponentsBuilder` for building URIs
- Catch `HttpClientErrorException.NotFound` and throw appropriate exceptions
- Follow nomenclature logging pattern

### Pagination
```java
// Controller
public ResponseEntity<Page<EntityResponse>> getAll(
    @PageableDefault(size = 20, sort = "name") Pageable pageable) {
    return ResponseEntity.ok(service.findAll(pageable));
}

// Service
public Page<EntityResponse> findAll(Pageable pageable) {
    return repository.findAll(pageable).map(mapper::toResponse);
}
```

### Name Uniqueness Check
```java
// Avoid duplicate on create
if (repository.existsByNameIgnoreCase(name)) {
    String message = messageService.getMessage("crud.already.exists", ENTITY_NAME, "name", name);
    throw new ResourceConflictException(message);
}

// Allow same name on update (skip check if name unchanged)
if (!existing.getName().equals(update.name()) && 
    repository.existsByNameIgnoreCase(update.name())) {
    String message = messageService.getMessage("crud.already.exists", ENTITY_NAME, "name", update.name());
    throw new ResourceConflictException(message);
}
```

### MapStruct Configuration
Use `componentModel = "spring"` for dependency injection:
```java
@Mapper(componentModel = "spring")
public interface EntityMapper {
    Entity toEntity(EntityRequest request);
    EntityResponse toResponse(Entity entity);
    void updateEntityFromDto(@MappingTarget Entity entity, EntityUpdate update);
}
```

## Critical Rules

1. **Always use `@Transactional`** on service methods that modify data
2. **Never expose entities directly** - always use DTOs
3. **Log at entry and exit** of service methods with nomenclature
4. **Validate external IDs** (e.g., verify UOM exists before saving) using service clients
5. **Use `Objects.requireNonNull()`** when null checks are critical
6. **Column names must be snake_case**, Java fields are camelCase
7. **IDs use `@Positive` validation**, never accept 0 or negative
8. **String fields need `@Size` limits** matching database constraints
9. **BigDecimal for money/quantities**, never use `float` or `double`
10. **Test isolation is mandatory** - use `reset()` in `@AfterEach`
11. **Throw proper exceptions with BaseException hierarchy** - use `MessageService` for message generation
12. **Never construct raw error messages** - always use `messageService.getMessage()` for localization

---

**Last Updated:** April 6, 2026  
**Version:** 1.1  
**Related:** See [TESTING_GUIDE.md](TESTING_GUIDE.md) for comprehensive testing patterns
