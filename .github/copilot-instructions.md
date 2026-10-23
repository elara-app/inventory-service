# Copilot instructions — inventory-service

See [AGENTS.md](../AGENTS.md) for the full agent guide. This file is a short pointer with the most-used commands and traps.

## Commands

```bash
sh ./mvnw test                                             # all tests (H2 test profile)
sh ./mvnw test -Dtest=InventoryItemImpTest#save_withValidRequest_createsAndReturnsResponse
sh ./mvnw -B clean verify -Dspring.profiles.active=test    # tests + JaCoCo check
```

The Maven wrapper is mode 644: run `sh ./mvnw`, never rely on an executable invocation, and do not `chmod +x` it.

## Key traps

- `BaseException` is concrete; exception constructors take a single message string.
- `findByNameIgnoreCase` does not exist; use `existsByNameIgnoreCase` or `findAllByNameContainingIgnoreCase`.
- Delete has no reference check; it never returns 409 "item in use".
- The UOM client maps only HTTP 404 to 404/1004; other failures become 500/1006. No retry, timeout or circuit breaker.
- No default pagination sort; page JSON is the nested `content` + `page` shape, never a flat Spring `Page`.
- Error codes: 1001 is `DATABASE_ERROR` (the integrity handler returns HTTP 409 with code 1001); 1005 `SERVICE_UNAVAILABLE` is declared but never thrown.
- OpenAPI page and delete-conflict examples are stale; live Swagger is authoritative.
- `spring-boot:run` needs Config Server and PostgreSQL, or the `test` profile.
