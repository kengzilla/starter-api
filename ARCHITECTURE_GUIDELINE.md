# Architecture Guideline

Rules every developer follows. When in doubt, check here first.

This document matches the **current** `starter-api` codebase. Example class names (`Bill`, `BillService`) illustrate patterns for **new** work; there is no example feature checked in yet.

---

## What exists today

| Area | Location / notes |
| --- | --- |
| Entry point | `com.starter.api.StarterApplication` |
| Response envelope | `core/common/ApiResponse.java` |
| Error codes (placeholder) | `core/common/ErrorCodes.java` — add `FEATURE_REASON` constants as you ship features |
| Business exceptions | `core/exception/ApiBusinessException.java`, `core/exception/GlobalExceptionHandler.java` |
| HTTP / docs | `core/config/CorsConfig.java`, `core/config/OpenApiConfig.java` (Springdoc) |
| Security | `core/security/` — package reserved; **no** interceptors or token checks are wired yet |
| Domain | `domain/entity/`, `domain/repository/` — placeholders (`package-info.java` only) |
| Features | `feature/` — add one subpackage per domain (e.g. `feature/auth`) |
| Integrations | `integration/api/`, `integration/storage/`, `integration/notification/` — outbound adapters |

REST controllers: add under `feature/<name>/` when you introduce each domain.

---

## Layer Structure

```
core/          Shared infrastructure — changes here affect the whole app
domain/        JPA entities and repositories — shared data layer
feature/       Business features — isolated, one subpackage per domain
integration/   Outbound adapters (external HTTP APIs, file storage, email/SMS, …)
```

### Dependency Direction

```
feature → domain → (database)
feature → core
feature → integration
core      → (nothing from feature, domain, or integration)
integration → core only (shared config/utilities), never feature
```

- A feature may use domain entities, core utilities, and integration clients.
- **Core** must never import from `feature`, `domain`, or `integration`.
- **Features** must never import from another feature’s package.
- Keep **integration** free of feature-specific types where possible (pass DTOs or primitives into integration clients).

---

## Response Envelope

Every endpoint returns `ApiResponse<T>` (see `core/common/ApiResponse.java`).

Shape is defined by `core/common/ApiResponse.java` (Jackson `JsonInclude.Include.NON_NULL` — null fields are omitted from JSON).

```json
// Success (ApiResponse.success(data) — message defaults to "OK")
{ "success": true, "data": { ... }, "message": "OK", "timestamp": "..." }

// Error (ApiResponse.error(code, message))
{ "success": false, "errorCode": "BILL_NOT_FOUND", "message": "...", "timestamp": "..." }
```

Use `ApiResponse.success(data, message)` when the client should see a custom success message. Use `ApiResponse.error(code, message, data)` when you need to attach payload on failure.

**Never** return a raw object, a plain `String`, or ad-hoc `ResponseEntity` bodies for business APIs without using `ApiResponse`.

---

## Error Codes

Add all business error codes to `ErrorCodes.java` as `public static final String` constants.

Naming convention: `FEATURE_REASON` in `UPPER_SNAKE_CASE`.

```java
// Good
BILL_NOT_FOUND
AUTH_INVALID_CREDENTIALS
PAYMENT_INSUFFICIENT_BALANCE

// Bad — too generic, no feature prefix
NOT_FOUND
ERROR
FAILED
```

Adding new codes: append to `ErrorCodes.java` with the feature prefix. **Never remove or rename** codes that clients or logs already rely on — the frontend maps these strings.

**Validation:** `GlobalExceptionHandler` currently returns error code `VALIDATION_ERROR` for `MethodArgumentNotValidException`. When you standardize codes, consider adding `VALIDATION_ERROR` (or `COMMON_VALIDATION`) to `ErrorCodes.java` and referencing it from the handler.

---

## Controller Rules

- No business logic — delegate to a service, return `ApiResponse.success(...)`.
- Use `@Valid` on every `@RequestBody` that carries input.
- Add `@Operation` and `@ApiResponses` on every endpoint (Springdoc).
- List endpoints must support pagination: `@RequestParam(defaultValue = "0") int page`, `@RequestParam(defaultValue = "20") int size` (unless product agrees otherwise).

```java
// Good
@PostMapping
public ApiResponse<BillResponse> create(@Valid @RequestBody BillDto.CreateRequest request) {
    return ApiResponse.success(billService.create(request));
}

// Bad — business logic in controller
@PostMapping
public ApiResponse<BillResponse> create(@RequestBody BillDto.CreateRequest request) {
    if (request.amount() == null) throw new RuntimeException("amount required");
    // ...
}
```

---

## Service Rules

- Business logic lives in feature services.
- `@Transactional` on write paths; `@Transactional(readOnly = true)` on reads where appropriate.
- Throw `ApiBusinessException` for expected business failures — avoid raw `RuntimeException` for those cases.
- Do not return JPA entities from service methods that back REST APIs — map to response DTOs/records first.

```java
// Good
@Transactional(readOnly = true)
public BillResponse findById(Long id) {
    Bill bill = billRepository.findById(id)
        .orElseThrow(() -> new ApiBusinessException(
            HttpStatus.NOT_FOUND, ErrorCodes.BILL_NOT_FOUND, "Bill not found"));
    return toResponse(bill);
}
```

---

## Entity Rules

- Prefer `@GeneratedValue(strategy = GenerationType.SEQUENCE)` over `IDENTITY` for portability (e.g. Oracle).
- Add `createdAt` and `updatedAt` on persistent entities unless there is a strong reason not to.
- Use `@Enumerated(EnumType.STRING)` — never `ORDINAL`.
- Use `BigDecimal` for money — never `double` or `float`.
- Lazy fetch on associations by default.

```java
// Primary key — Oracle-friendly sequence
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_seq")
@SequenceGenerator(name = "entity_seq", sequenceName = "ENTITY_NAME_SEQ", allocationSize = 1)
private Long id;
```

---

## Repository Rules

- Prefer Spring Data derived query methods or `@Query` with **JPQL**.
- Avoid native SQL unless you accept portability trade-offs and document them.
- Name methods for what they return: `findByEmailIgnoreCase`, `countByStatus`.

```java
// Good — JPQL
@Query("SELECT COALESCE(SUM(b.amount), 0) FROM Bill b WHERE b.userId = :userId")
BigDecimal sumAmountByUser(@Param("userId") Long userId);

// Avoid — native SQL breaks DB portability
@Query(value = "SELECT SUM(amount) FROM bills WHERE user_id = ?", nativeQuery = true)
BigDecimal sumAmountByUser(Long userId);
```

---

## Database Portability

The starter defaults to an **Oracle** JDBC driver but is intended to work with any JDBC database you configure.

Rules:

- Prefer `SEQUENCE` for primary keys where the target DB supports it.
- Use JPQL in `@Query` where possible.
- Avoid DB-specific functions in JPQL (`ROWNUM`, `TOP`, `LIMIT`, …).
- Prefer Spring Data `Pageable` over hand-written paging SQL.

---

## Database Schema Management (Flyway)

Flyway is the source of truth for schema in **local** and shared environments (see `README.md` profiles).

Rules:

- Every schema change is a **new** file under `src/main/resources/db/migration/`.
- Naming: `V{n}__{description}.sql` (e.g. `V1__create_bills_table.sql`).
- **Never** edit a migration that has already been applied anywhere that matters.
- Do not rely on `ddl-auto=update` in shared or production environments.

Profile expectations:

- **`local`**: Flyway enabled, `spring.jpa.hibernate.ddl-auto=validate` (see `application-local.properties.example`).
- **`test`**: Flyway disabled, `create-drop` for fast tests (unless you add tests that run migrations explicitly).

---

## Security (current state and next steps)

**Today:** there is **no** Spring Security filter chain, **no** JWT/OAuth2, and **no** `HandlerInterceptor` registered. `CorsConfig` allows `http://localhost:*` for development only.

**When you add authentication (incrementally):**

1. Introduce a `feature/<name>/` slice (e.g. `feature/auth`) with controller → service → DTOs → repositories as needed.
2. Register a `HandlerInterceptor` (or Spring Security) from `core/config` or `core/security`, and keep protected path patterns explicit (e.g. public `/api/v1/auth/**`, protected `/api/v1/**`).
3. Use strong password hashing (e.g. BCrypt) for real credentials; never log passwords or raw `Authorization` headers.
4. Tighten CORS for non-local environments (replace permissive dev rules before production).

Document mock vs real token behaviour in the feature’s README or PR until production auth is in place.

---

## Definition of Done

For each new REST feature, confirm:

- [ ] Controller delegates to a service — no business rules in the controller.
- [ ] Responses use `ApiResponse<T>`.
- [ ] Business failures use `ApiBusinessException` with a constant from `ErrorCodes.java`.
- [ ] New error codes use a feature prefix and are added to `ErrorCodes.java`.
- [ ] `@Valid` on request bodies that need validation.
- [ ] Appropriate `@Transactional` on service methods.
- [ ] No JPA entity returned directly as the public API body.
- [ ] Endpoints documented with `@Operation` and `@ApiResponses`.
- [ ] Tests cover success and each meaningful error path.
- [ ] Schema changes ship as new Flyway migrations (no hand-edited applied migrations).
