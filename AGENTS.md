# AGENTS.md — instructions for AI coding agents

## Project overview
Bank expense-limit microservice (Java 21, Spring Boot 3.4).  
Two REST APIs + MCP tool surface. PostgreSQL + Flyway, optional Redis, external FX (Twelve Data), pessimistic locking.

## Build & test
```bash
mvn -q verify          # needs Docker for Testcontainers
mvn -q spring-boot:run
docker compose up --build
```

## Package layout
- `client/` — `ExchangeRateProvider` interface
- `client/impl/` — TwelveData (and future providers)
- `service/` — `*ServiceApi` interfaces
- `service/impl/` — implementations
- `controller/` — REST
- `mcp/` — MCP-style tool endpoints for AI agents
- `repository/`, `entity/`, `dto/`, `mapper/`, `config/`, `exception/`

## Conventions
- Depend on interfaces (`LimitServiceApi`, `ExchangeRateProvider`, …), not concrete classes.
- Records for DTOs; `Clock` bean for “now”; month boundaries in `Asia/Almaty`.
- Limits are append-only; external rate failures never block transaction acceptance.
- Redis optional (`app.redis.enabled`); without it only DB cache is used.

## Key test
`EndToEndCase1Test` — case 1 from the assignment via HTTP + Testcontainers + WireMock.
