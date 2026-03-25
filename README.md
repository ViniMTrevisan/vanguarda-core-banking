# vanguarda-core-banking

Core banking transfer service focused on **consistency**, **idempotency**, and **operability**.

## Problem this project addresses

Account-to-account transfer systems have a few hard requirements:

- prevent balance corruption under concurrent requests;
- avoid duplicate processing on retries/timeouts;
- keep auditable records for every movement;
- expose operational signals for debugging and incident response.

This project is built around those constraints.

## What the service does

- manages accounts (`ACTIVE`, `FROZEN`, `CLOSED`);
- executes transfers with mandatory `X-Idempotency-Key`;
- writes one `transactions` record plus two `ledger_entries` (debit/credit);
- supports balance and statement queries;
- publishes `TransactionCompletedEvent` to RabbitMQ;
- exposes health and Prometheus metrics endpoints.

## Why this design was chosen

- **Clean Architecture:** isolate business rules from frameworks and adapters, making behavior easier to test and evolve.
- **`SERIALIZABLE` transaction isolation:** strongest protection against write anomalies during money movement.
- **Distributed lock + DB lock combination:** Redis/Redlock coordinates across instances; `SELECT ... FOR UPDATE` in PostgreSQL protects row-level consistency.
- **Deterministic lock order:** source/target account locks are acquired in UUID lexical order to reduce deadlock risk.
- **Redis-backed idempotency:** explicit `PROCESSING` and `COMPLETED` states to handle retries and replay safely.
- **Double-entry ledger model:** every transfer produces mirrored debit/credit entries with `balanceBefore` and `balanceAfter`.
- **Observability-first metrics:** business and latency metrics are exported and dashboarded by default.

## How it is implemented

### Architecture

This project follows **Clean Architecture** principles:

```
src/main/java/com/vinicius/vanguarda/
├── domain/ # Business model, value objects, ports, domain exceptions
│ ├── model/
│ ├── repository/
│ └── service/
├── application/ # Use case orchestration
│ └── usecase/
└── infrastructure/ # Framework and external adapters
  ├── persistence/ # JPA/PostgreSQL
  ├── cache/       # Redis idempotency
  ├── lock/        # Redisson lock provider
  ├── messaging/   # RabbitMQ event publishing
  ├── metrics/     # Micrometer custom metrics
  └── web/         # REST controllers + exception handling
```

### Transfer execution flow

`POST /v1/transactions` executes this path:

1. validate `X-Idempotency-Key` (UUID);
2. check idempotency cache (`COMPLETED` replay or `PROCESSING` conflict);
3. acquire distributed locks in deterministic order (`vcb:lock:<uuid>`);
4. run transfer in a `SERIALIZABLE` DB transaction:
   - row lock both accounts (`findByIdForUpdate`);
   - validate account status, funds, and currency;
   - persist transaction + debit/credit ledger entries;
   - persist updated balances;
5. cache response as idempotency `COMPLETED`;
6. publish completion event to RabbitMQ;
7. return `201 Created` (first execution) or `200 OK` + `X-Idempotency-Replayed: true` (replay).

### Data model (PostgreSQL + Flyway)

- `accounts`: owner, currency, balance, status, optimistic version, timestamps.
- `transactions`: idempotency key (unique), source/target accounts, amount, status, metadata.
- `ledger_entries`: transaction/account relation, debit/credit type, amount, balance snapshots.

Schema constraints include positive amounts, non-negative balances, and `source_account_id != target_account_id`.

### Error contract

Errors are normalized by `GlobalExceptionHandler` into `ApiError`:

- fields: `timestamp`, `status`, `error`, `code`, `message`, `path`, `traceId`;
- examples of codes: `MISSING_IDEMPOTENCY_KEY`, `INSUFFICIENT_BALANCE`, `TRANSACTION_IN_PROGRESS`, `CURRENCY_MISMATCH`.

### Observability

- Metrics endpoint: `/actuator/prometheus`
- Provisioned stack: Prometheus + Grafana
- Key metrics:
  - `vcb_transactions_total{status="COMPLETED|FAILED"}`
  - `vcb_idempotency_hits_total{replayed="true|false"}`
  - `vcb_transactions_duration_seconds_*`
  - `vcb_balance_query_duration_seconds_*`
  - `vcb_distributed_lock_failures_total`

## Tech stack

- Java 21
- Spring Boot 3.4
- PostgreSQL 16
- Redis 7
- RabbitMQ 3.13
- Flyway
- Micrometer + Prometheus + Grafana
- JUnit 5, Mockito, Testcontainers
- Docker / Docker Compose

## Quick Start

Run the full local stack:

```bash
docker compose up -d --build
```

Or run app only (if dependencies are already running):

```bash
./mvnw spring-boot:run
```

## Manual test guide (local, without automated tests)

If your containers are already up, use this checklist to validate the system manually.

### 1) Validate that everything is running

```bash
docker compose ps
```

Expected ports:
- App: `http://localhost:8080`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- RabbitMQ UI: `http://localhost:15672`

Quick health checks:

```bash
curl -sS http://localhost:8080/actuator/health
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui/index.html
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/api-docs
```

### 2) Access Grafana and Prometheus

Prometheus:
- URL: `http://localhost:9090`
- Check target status in `Status -> Targets` (job `vcb` should be `UP`).
- Or by API:

```bash
curl -sS http://localhost:9090/api/v1/targets
```

Grafana:
- URL: `http://localhost:3000`
- Login: `admin` / `admin`
- Datasource `Prometheus` is provisioned automatically.
- Dashboard `VCB — Core Banking` is provisioned automatically.

### 3) Test the API manually with curl (happy path)

Set the base URL:

```bash
BASE_URL=http://localhost:8080
```

Create account A:

```bash
curl -sS -X POST "$BASE_URL/v1/accounts" \
  -H 'Content-Type: application/json' \
  -d '{
    "ownerId": "user-a",
    "ownerName": "User A",
    "currency": "BRL",
    "initialBalance": 1000.00
  }'
```

Create account B:

```bash
curl -sS -X POST "$BASE_URL/v1/accounts" \
  -H 'Content-Type: application/json' \
  -d '{
    "ownerId": "user-b",
    "ownerName": "User B",
    "currency": "BRL",
    "initialBalance": 200.00
  }'
```

Save `accountId` values from both responses as `ACCOUNT_A` and `ACCOUNT_B`.

Transfer money (`X-Idempotency-Key` is required and must be a UUID):

```bash
IDEMPOTENCY_KEY=$(python3 -c "import uuid; print(uuid.uuid4())")

curl -i -sS -X POST "$BASE_URL/v1/transactions" \
  -H 'Content-Type: application/json' \
  -H "X-Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "{
    \"sourceAccountId\": \"$ACCOUNT_A\",
    \"targetAccountId\": \"$ACCOUNT_B\",
    \"amount\": 150.00,
    \"description\": \"Manual transfer\",
    \"metadata\": {\"channel\": \"curl\"}
  }"
```

Read balances:

```bash
curl -sS "$BASE_URL/v1/accounts/$ACCOUNT_A/balance"
curl -sS "$BASE_URL/v1/accounts/$ACCOUNT_B/balance"
```

List account statement:

```bash
curl -sS "$BASE_URL/v1/accounts/$ACCOUNT_A/transactions?page=0&size=20"
```

### 4) Error scenarios you should validate

Missing idempotency key (expect `400`):

```bash
curl -i -sS -X POST "$BASE_URL/v1/transactions" \
  -H 'Content-Type: application/json' \
  -d "{
    \"sourceAccountId\": \"$ACCOUNT_A\",
    \"targetAccountId\": \"$ACCOUNT_B\",
    \"amount\": 10.00
  }"
```

Insufficient balance (expect `422`):

```bash
curl -i -sS -X POST "$BASE_URL/v1/transactions" \
  -H 'Content-Type: application/json' \
  -H "X-Idempotency-Key: $(python3 -c \"import uuid; print(uuid.uuid4())\")" \
  -d "{
    \"sourceAccountId\": \"$ACCOUNT_A\",
    \"targetAccountId\": \"$ACCOUNT_B\",
    \"amount\": 999999.00
  }"
```

Idempotency replay (same key + same payload, expect `200` and header `X-Idempotency-Replayed: true`):

```bash
curl -i -sS -X POST "$BASE_URL/v1/transactions" \
  -H 'Content-Type: application/json' \
  -H "X-Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "{
    \"sourceAccountId\": \"$ACCOUNT_A\",
    \"targetAccountId\": \"$ACCOUNT_B\",
    \"amount\": 150.00,
    \"description\": \"Manual transfer\",
    \"metadata\": {\"channel\": \"curl\"}
  }"
```

### 5) Verify observability after requests

Check exposed metrics:

```bash
curl -sS http://localhost:8080/actuator/prometheus | grep '^vcb_'
```

Useful metrics to inspect:
- `vcb_transactions_total{status="COMPLETED"}`
- `vcb_transactions_total{status="FAILED"}`
- `vcb_idempotency_hits_total{replayed="true"}`
- `vcb_idempotency_hits_total{replayed="false"}`
- `vcb_transactions_duration_seconds_*`
- `vcb_balance_query_duration_seconds_*`
- `vcb_distributed_lock_failures_total`

In Prometheus expression browser (`http://localhost:9090`), try:

```promql
sum(rate(vcb_transactions_total[1m]))
sum(rate(vcb_transactions_total{status="FAILED"}[1m]))
sum(rate(vcb_idempotency_hits_total{replayed="true"}[5m]))
```

In Grafana (`http://localhost:3000`), open dashboard `VCB — Core Banking` and verify panels react after running the curl commands.

### 6) Test with Postman

You can import:
- OpenAPI from `http://localhost:8080/api-docs`, or
- Collection file in this repository:
  - `postman/vanguarda-core-banking-local.postman_collection.json`

Recommended Postman variables:
- `baseUrl`: `http://localhost:8080`
- `accountAId`
- `accountBId`
- `transactionId`
- `idempotencyKey`

Collection requests include:
- health check
- create/list/get account
- transfer transaction
- idempotency replay
- missing idempotency key
- insufficient balance
- prometheus scrape endpoint

### 7) Troubleshooting

- `400 MISSING_IDEMPOTENCY_KEY`: header missing or not a valid UUID format.
- `422 INSUFFICIENT_BALANCE`: transfer amount is bigger than source account balance.
- Grafana empty panels: run a few transactions first, then check Prometheus target `vcb` is `UP`.
- App not reachable on `8080`: confirm `docker compose ps` and check app logs with `docker compose logs app --tail=200`.

## Automated tests

Run all tests:

```bash
./mvnw test
```

Run CI-style checks:

```bash
./mvnw clean verify
```

Run targeted suites:

```bash
./mvnw test -Dtest=TransferMoneyUseCaseTest
./mvnw test -Dtest=TransactionControllerTest
./mvnw test -Dtest=TransferIntegrationTest
./mvnw test -Dtest=ConcurrentTransferTest
```

## Design trade-offs

- If Redis is temporarily unavailable, transfer flow can still proceed using DB locking strategy; this favors availability while keeping core consistency protection in PostgreSQL.
- RabbitMQ publish failures are recorded in metrics/logs and do not roll back committed transfers; transfer consistency is prioritized over synchronous event delivery.
