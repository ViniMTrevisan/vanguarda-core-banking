# Copilot Instructions for vanguarda-core-banking

## Build, run, and test commands

- Run app locally (requires PostgreSQL, Redis, RabbitMQ running):
  - `./mvnw spring-boot:run`
- Start local dependencies:
  - `docker-compose up -d postgres redis rabbitmq`
- Build JAR:
  - `./mvnw clean package`
- Run full test suite:
  - `./mvnw test`
- Run CI-style checks:
  - `./mvnw clean verify`
- Run a single test class:
  - `./mvnw test -Dtest=TransferMoneyUseCaseTest`
- Run a single test method:
  - `./mvnw test -Dtest=TransferMoneyUseCaseTest#shouldReturnCachedResponseOnReplay`

Notes:
- There is no separate lint command configured in `pom.xml` (no Checkstyle/SpotBugs/PMD/Spotless plugins).
- Flyway migrations run on startup (`spring.flyway.enabled=true`) and JPA uses `ddl-auto: validate`.

## High-level architecture

This service implements a core-banking transfer flow using a Clean Architecture split:

- `domain/`: pure business model, exceptions, repository/service ports.
  - Key models: `Account`, `Transaction`, `LedgerEntry`, `Money`.
  - Ports: `AccountRepository`, `TransactionRepository`, `LedgerEntryRepository`, `IdempotencyProvider`, `DistributedLockProvider`, `EventPublisher`.
- `application/usecase/`: transactional orchestration and use-case outputs as Java records.
  - `TransferMoneyUseCase` is the center of money movement.
- `infrastructure/`: adapters for web, persistence, cache, lock, messaging, and metrics.
  - REST controllers: `infrastructure/web/controller`.
  - JPA adapters/entities: `infrastructure/persistence`.
  - Redis idempotency: `infrastructure/cache/RedisIdempotencyProvider`.
  - Redlock adapter: `infrastructure/lock/RedlockDistributedLockProvider`.
  - RabbitMQ publisher/config: `infrastructure/messaging`.
  - API error contract: `infrastructure/web/exception`.

Transfer execution path (`POST /v1/transactions`):
1. Requires `X-Idempotency-Key` (UUID string).
2. Checks Redis idempotency state (`PROCESSING` vs `COMPLETED` cached payload).
3. Acquires distributed locks in deterministic UUID-lexicographic order (`vcb:lock:<uuid>`), then acquires DB row locks via `findByIdForUpdate`.
4. Executes debit/credit and writes one `transactions` row plus two `ledger_entries` rows inside a `SERIALIZABLE` transaction.
5. Caches response for replay and publishes `TransactionCompletedEvent` to RabbitMQ.
6. Returns `201 Created` for first execution or `200 OK` with `X-Idempotency-Replayed: true` on replay.

Data/infra components:
- PostgreSQL + Flyway migrations in `src/main/resources/db/migration`.
- Redis for idempotency cache/state.
- RabbitMQ for async transaction events (with DLX/DLQ config).
- Micrometer + Actuator Prometheus endpoint for transfer metrics.

## Key conventions in this codebase

- Keep dependency direction strict: domain <- application <- infrastructure.
- Use case interfaces are implicit through class methods; inputs/outputs are Java `record`s inside each use case class.
- Domain/persistence mapping pattern:
  - Domain creation methods (e.g., `Account.create`, `Transaction.create`) for new objects.
  - `restore(...)` methods for rehydration from persistence entities.
  - Infrastructure entity classes own `toDomain()` / `fromDomain(...)` conversions.
- Money and amounts:
  - Use `Money` value object for arithmetic and currency checks.
  - Monetary scale is normalized to 2 decimals (`HALF_UP`).
  - Request DTOs enforce `@Digits(..., fraction = 2)` and positive amounts.
- Concurrency and consistency:
  - `TransferMoneyUseCase` runs with `@Transactional(isolation = Isolation.SERIALIZABLE)`.
  - Always lock account pair in deterministic order to avoid deadlocks.
  - Keep both distributed lock + DB pessimistic lock strategy (`findByIdForUpdate`).
- Idempotency contract:
  - `X-Idempotency-Key` is mandatory for transfers.
  - Replay behavior must preserve payload and add `X-Idempotency-Replayed: true`.
  - Redis key namespace is `idempotency:` with separate PROCESSING and COMPLETED semantics.
- Error response shape is standardized via `ApiError` (`timestamp`, `status`, `error`, `code`, `message`, `path`, `traceId`) and mapped in `GlobalExceptionHandler`.
- Controller pagination convention:
  - `page` default 0, `size` default 20, hard cap `size <= 100`, sorted by `createdAt desc`.
- Test layering convention:
  - Domain unit tests under `src/test/java/.../domain`.
  - Use-case tests with Mockito (`@ExtendWith(MockitoExtension.class)`).
  - Web slice tests with `@WebMvcTest`.
  - Integration tests use Testcontainers (`PostgreSQL`, `RabbitMQ`, `Redis`) and `@ActiveProfiles("test")`.
