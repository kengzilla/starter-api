# Architecture Guideline

Rules every developer follows. When in doubt, check here first.

This document matches the **current** `starter-api` codebase and its modular-monolith **Phase 1** baseline.

---

## What exists today

| Area | Location / notes |
| --- | --- |
| Entry point | `com.starter.api.StarterApplication` |
| App composition | `app/` (`config`, `exception`, `security`) |
| Shared kernel | `shared/api/ApiResponse.java`, `shared/error/ErrorCodes.java`, `shared/exception/ApiBusinessException.java` |
| Health endpoint | `modules/health/api/HealthController.java` exposes `GET /api/v1/health` |
| Auth module | `modules/auth/api`, `modules/auth/domain`, `modules/auth/infra` (skeleton only) |
| Security | `app/security/SecurityConfig` is a placeholder; no real auth chain yet |

---

## Module Structure (Phase 1)

```
app/       Application composition and cross-cutting web configuration
shared/    Shared kernel used by multiple modules
modules/
  ├── health/  Lightweight liveness API
  └── auth/    Authentication module skeleton (api/domain/infra)
```

Planned next phase (documented, not implemented yet):

```
facility/  floorplan/  interest/  payment/
```

### Dependency Direction

```
app            -> modules.* and shared (compose modules)
modules.auth   -> shared
modules.health -> shared
shared         -> (must not depend on modules.* / business modules)
```

Rules:

- Cross-module calls must use module public contracts (typically `api` package).
- Do not import another module's `infra` package directly.
- External-system adapters belong to the owning module's `infra` package (for example `modules/auth/infra`), not a global integration root.
- Keep `shared` free from business-specific logic.

---

## Response Envelope

Every endpoint returns `ApiResponse<T>` (see `shared/api/ApiResponse.java`).

Shape is defined by `shared/api/ApiResponse.java` (Jackson `JsonInclude.Include.NON_NULL` - null fields are omitted from JSON).

```json
// Success (ApiResponse.success(data) - message defaults to "OK")
{ "success": true, "data": { ... }, "message": "OK", "timestamp": "..." }

// Error (ApiResponse.error(code, message))
{ "success": false, "errorCode": "BILL_NOT_FOUND", "message": "...", "timestamp": "..." }
```

Use `ApiResponse.success(data, message)` when the client should see a custom success message. Use `ApiResponse.error(code, message, data)` when you need to attach payload on failure.

**Never** return a raw object, a plain `String`, or ad-hoc `ResponseEntity` bodies for business APIs without using `ApiResponse`.

---

## Error Codes

Add all business error codes to `shared/error/ErrorCodes.java` as `public static final String` constants.

Naming convention: `FEATURE_REASON` in `UPPER_SNAKE_CASE`.

```java
// Good
BILL_NOT_FOUND
AUTH_INVALID_CREDENTIALS
PAYMENT_INSUFFICIENT_BALANCE

// Bad - too generic, no feature prefix
NOT_FOUND
ERROR
FAILED
```

Adding new codes: append to `ErrorCodes.java` with the feature prefix. **Never remove or rename** codes that clients or logs already rely on.

---

## Controller Rules

- No business logic - delegate to a service, return `ApiResponse.success(...)`.
- Use `@Valid` on every `@RequestBody` that carries input.
- Add `@Operation` and `@ApiResponses` on every endpoint (Springdoc).
- List endpoints should support pagination where applicable (`page` / `size`).

---

## Service Rules

- Business logic lives in module domain services.
- `@Transactional` on write paths; `@Transactional(readOnly = true)` on reads where appropriate.
- Throw `ApiBusinessException` for expected business failures.
- Do not return JPA entities directly from API-facing service methods.

---

## Repository / Entity Rules

- Prefer Spring Data derived query methods or `@Query` with JPQL.
- Avoid native SQL unless you accept portability trade-offs and document them.
- Use `BigDecimal` for money.
- Use `@Enumerated(EnumType.STRING)` for enums.
- Prefer lazy fetch on associations by default.

---

## Database Schema Management (Flyway)

Flyway is the source of truth for schema in local and shared environments.

Rules:

- Every schema change is a **new** file under `src/main/resources/db/migration/`.
- Naming: `V{n}__{description}.sql`.
- **Never** edit a migration that has already been applied.
- Do not rely on `ddl-auto=update` in shared or production environments.

---

## Security (current state and next steps)

**Today:**

- `app/security/SecurityConfig` exists as a placeholder only.
- There is **no** Spring Security filter chain, **no** JWT/OAuth2, and **no** auth interceptor wired.
- CORS currently allows `http://localhost:*` for development.

**Next step for auth module:**

1. Implement contracts in `modules/auth/api`.
2. Add business rules and token/policy logic in `modules/auth/domain`.
3. Add persistence/external adapters in `modules/auth/infra`.
4. Wire real security in `app/security` and keep public/protected routes explicit.

---

## Health endpoint contract

- Endpoint: `GET /api/v1/health`
- Purpose: lightweight liveness signal for server/runtime
- Expected payload: service status (`UP`), service name, timestamp

---

## Checklist: Adding a New Module

When adding a new module under `modules/<name>/`, use this minimum checklist:

- [ ] Create package structure: `modules/<name>/api`, `modules/<name>/domain`, `modules/<name>/infra`.
- [ ] Put controllers/public module contracts in `api`.
- [ ] Put business rules and orchestration in `domain` (not in controllers).
- [ ] Put database/external adapters in `infra` (for example REST clients, storage, messaging).
- [ ] Keep cross-module calls on `api` contracts only; do not import another module's `infra`.
- [ ] If logic is generic and reused by many modules, move it to `shared` (and keep it business-agnostic).
- [ ] Add module-specific tests (controller/service paths and meaningful error cases).
- [ ] Update `README.md` and this guideline when module boundaries or contracts change.

---

## Definition of Done

For each new REST feature, confirm:

- [ ] Controller delegates to a service - no business rules in controller.
- [ ] Responses use `ApiResponse<T>`.
- [ ] Business failures use `ApiBusinessException` with a constant from `ErrorCodes.java`.
- [ ] New error codes use a feature prefix and are added to `ErrorCodes.java`.
- [ ] `@Valid` on request bodies that need validation.
- [ ] Appropriate `@Transactional` on service methods.
- [ ] No JPA entity returned directly as the public API body.
- [ ] Endpoints documented with `@Operation` and `@ApiResponses`.
- [ ] Tests cover success and each meaningful error path.
- [ ] Schema changes ship as new Flyway migrations.
