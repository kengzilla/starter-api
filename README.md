# Starter API

Spring Boot 3.5 / Java 21 backend starter template.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Data JPA + Hibernate
- Flyway (database schema migrations)
- Spring Validation
- Oracle (primary) - switchable to any JDBC-compatible database
- H2 in-memory (tests and default dev profile)
- Springdoc OpenAPI (Swagger UI)
- Lombok

## Modular Monolith (Phase 1)

This starter now uses a phase-1 modular-monolith baseline:

- `app/` for application composition and global concerns
- `shared/` for shared kernel (response envelope, common exceptions, cross-cutting utilities)
- `modules/auth/` as an empty module skeleton (`api/domain/infra`)
- `modules/health/` for lightweight health endpoint

Business modules in the target architecture (`facility`, `floorplan`, `interest`, `payment`) are documented as the next phase and are not implemented in code yet.

Legacy placeholder roots from the old structure (`core`, `domain`, `feature`, `integration`, and root-level `auth`/`health`) were removed.
For external systems, keep adapters inside each owning module's `infra` package (for example `modules/auth/infra`).

## Quick Start: Add a New Module

Use this baseline when introducing `modules/<name>/`:

1. Create three packages:
   - `modules/<name>/api`
   - `modules/<name>/domain`
   - `modules/<name>/infra`
2. Put public contracts/controllers in `api`.
3. Put business rules in `domain` (keep controllers thin).
4. Put DB/external-system adapters in `infra`.
5. Call other modules via their `api` contracts only (never import another module's `infra`).
6. Move truly generic reusable logic to `shared` (business-agnostic only).
7. Add/update tests and document boundary changes in `ARCHITECTURE_GUIDELINE.md`.

Example (`modules/payment`):

```text
src/main/java/com/starter/api/modules/payment/
├── api/       PaymentController, PaymentFacade
├── domain/    PaymentService, PaymentPolicy, Receipt
└── infra/     PaymentRepository, BankGatewayClient
```

## Project Structure

```
src/main/java/com/starter/api/
├── StarterApplication.java          Entry point
├── app/
│   ├── config/                      CORS, OpenAPI and app-level configuration
│   ├── exception/                   Global exception mapping
│   └── security/                    Security bootstrap placeholder
├── shared/
│   ├── api/                         ApiResponse and shared response contracts
│   ├── error/                       Error code constants
│   └── exception/                   Shared business exception types
└── modules/
    ├── health/
    │   └── api/                     Health endpoint (`/api/v1/health`)
    └── auth/
        ├── api/                     Public auth contracts (skeleton)
        ├── domain/                  Auth business domain (skeleton)
        └── infra/                   Auth infrastructure adapters (skeleton)
```

## Correlation and logging (brief)

- **Inbound HTTP:** [`CorrelationIdFilter`](src/main/java/com/starter/api/app/logging/CorrelationIdFilter.java) accepts **`X-Request-Id`** / **`X-Correlation-Id`**, stores **`requestId`** in MDC, and returns the id on the response. Structured JSON logs include `requestId`, `traceId`, and `spanId` when tracing is active (see [`docs/LOGGING_USE_CASES.md`](docs/LOGGING_USE_CASES.md)).
- **Outbound HTTP:** [`RestClientRequestIdPropagationConfig`](src/main/java/com/starter/api/app/config/RestClientRequestIdPropagationConfig.java) registers a **`RestClientCustomizer`** so `RestClient` instances built from the auto-configured `RestClient.Builder` forward **`X-Request-Id`** from the current MDC to downstream services.

## Health Endpoint

The starter provides a lightweight liveness endpoint:

```http
GET /api/v1/health
```

Example success response:

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "starter-api",
    "checkedAt": "2026-05-07T03:00:00Z"
  },
  "message": "OK",
  "timestamp": "2026-05-07T03:00:00Z"
}
```

## Getting Started

### 1. Configure your database (local profile only)

Copy `application-local.properties.example` to `application-local.properties` (gitignored) and fill in your connection details.

The example file has ready-to-use templates for Oracle, PostgreSQL, and MySQL - uncomment the one you need.

### 2. Run the application

```bash
# Default: test profile (H2 in-memory, no external database)
./mvnw spring-boot:run

# Explicit test profile (same as default)
./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# Local profile (requires application-local.properties + JDBC driver)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

On local profile, Flyway runs migrations from `src/main/resources/db/migration` automatically before JPA validation.

### 3. Run tests

```bash
./mvnw test
```

Tests use an H2 in-memory database - no external database needed.

### 4. Open Swagger UI

```
http://localhost:8080/swagger-ui.html
```

## Switching the Database Driver

The default driver is Oracle (`ojdbc11`). To switch:

1. In `pom.xml`, replace the `ojdbc11` dependency with your driver:

```xml
<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

2. In `application-local.properties`, uncomment the matching connection block.

## Port

Default: `8080`. Change in `application.properties` with `server.port=XXXX`.

## Database Migrations (Flyway)

This project uses Flyway as the single source of truth for schema changes.

- Local profile (`application-local.properties`):
  - `spring.flyway.enabled=true`
  - `spring.flyway.locations=classpath:db/migration`
  - `spring.jpa.hibernate.ddl-auto=validate`
- Test profile (`application-test.properties`):
  - `spring.flyway.enabled=false`
  - `spring.jpa.hibernate.ddl-auto=create-drop`

### Migration file naming

Use: `V{version}__{description}.sql`

Examples: `V1__create_some_table.sql`, `V2__add_column_to_some_table.sql`

### Typical workflow

1. Create a new SQL file under `src/main/resources/db/migration`
2. Put only forward-only schema changes in the file
3. Run the app with local profile
4. Flyway applies pending migrations in order
5. Commit code + migration in the same PR

### Important rules

- Never edit an already-applied migration file
- Never rely on `ddl-auto=update` in shared/real environments
- If schema needs change, add a new migration with the next version
