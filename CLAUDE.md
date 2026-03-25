# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run application (requires PostgreSQL running)
./mvnw spring-boot:run

# Build JAR
./mvnw clean package

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassName

# Build and run all checks (CI-style)
./mvnw clean verify

# Start local PostgreSQL database
docker-compose up -d

# Build Docker image
docker build -t vanguarda-core-banking:latest .
```

Application runs on `http://localhost:8080`. Tests use H2 in-memory; runtime uses PostgreSQL 15 via Docker Compose.

## Architecture

**Clean Architecture** with three layers:

- **`domain/`** — Framework-agnostic. Contains entities, repository interfaces (ports), and use case classes. No Spring annotations here. Use cases take `Input` records and return `Output` records.
- **`application/`** — Orchestration services (e.g., `AuthService`) and payment use case. Depends only on domain layer.
- **`infrastructure/`** — Adapters: JPA persistence, JWT/security, REST controllers, Stripe gateway. Wired via Spring DI.

**Dependency rule:** domain ← application ← infrastructure. Infrastructure implements domain interfaces.

### Key patterns

- Domain entities use factory methods: `User.create()` (new user) vs `User.restore()` (from DB)
- Repository interfaces in `domain/repository/`; JPA implementations in `infrastructure/persistence/`
- Port interfaces for cross-cutting concerns: `PasswordEncoder`, `TokenGenerator` (in domain), implemented in `infrastructure/security/SecurityAdapters.java`
- Java records for all DTOs (Input/Output, AuthResponse)

### Missing implementations (stubs referenced but not yet written)

- `StripeGateway` — referenced in `PaymentUseCase` but not implemented
- `UserEntity` + `JpaUserRepository` — referenced in `PostgresUserRepository` but not implemented
- Spring Security configuration bean (JWT filter/validator absent)
- No tests exist yet (`src/test/` is empty)

## Configuration

**Required environment variables (not set by default):**

| Variable | Purpose |
|---|---|
| `application.security.jwt.secret-key` | JWT signing secret (empty in `application.properties`) |
| `STRIPE_SECRET_KEY` | Stripe API key |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |
| `FRONTEND_URL` | Stripe redirect URL (defaults to `http://localhost:3000`) |

Database URL in `application.properties` uses hostname `postgres` (matches Docker Compose service name). For local dev outside Docker, override with `localhost`.

## Infrastructure

Terraform modules in `infra/` provision AWS resources: VPC, ECS Fargate, RDS PostgreSQL, ALB, ECR. CI/CD pipeline in `.github/workflows/ci-cd.yml` is currently commented out.

## PRD Context

The PRD (`PRD.md`) describes the intended full system: double-entry ledger, idempotency engine (Redis + `X-Idempotency-Key`), distributed locking (Redlock), event publishing (RabbitMQ), and real-time balance queries. The current codebase is an early scaffold — only auth and payment skeleton are in place. New domain entities (Account, Transaction, Ledger) and their use cases should follow the same Clean Architecture pattern as `User`.
