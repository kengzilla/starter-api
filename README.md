# Starter API

Spring Boot 3.5 / Java 21 backend starter template.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Data JPA + Hibernate
- Flyway (database schema migrations)
- Spring Validation
- Oracle (primary) — switchable to any JDBC-compatible database
- H2 in-memory (tests and default dev profile)
- Springdoc OpenAPI (Swagger UI)
- Lombok

## Project Structure

```
src/main/java/com/starter/api/
├── StarterApplication.java       Entry point
├── core/
│   ├── common/                   Shared response envelope and error codes
│   ├── config/                   CORS, OpenAPI, and other cross-cutting beans
│   ├── exception/                Business exception and global error handler
│   └── security/                 Reserved for interceptors / token checks when you add auth
├── domain/
│   ├── entity/                   JPA entities
│   └── repository/               Spring Data repositories
├── feature/                      One subpackage per domain (auth, bill, …) — add as you go
├── integration/
│   ├── api/                      # ต่อกับ REST API ภายนอก (เช่น Stripe, Firebase)
│   ├── storage/                  # ต่อกับระบบไฟล์ (เช่น AWS S3)
│   └── notification/             # ต่อกับระบบส่ง Email / SMS
```

## Getting Started

### 1. Configure your database (local profile only)

Copy `application-local.properties.example` to `application-local.properties` (gitignored) and fill in your connection details.

The example file has ready-to-use templates for Oracle, PostgreSQL, and MySQL — uncomment the one you need.

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

Tests use an H2 in-memory database — no external database needed.

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
