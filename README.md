# Limit Service — Junior Java Developer Test Assignment (2026)

Микросервис учёта расходных операций и месячных лимитов в USD.

## Стек

| Компонент | Технология |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| DB | PostgreSQL 16 + Flyway |
| ORM | Spring Data JPA |
| HTTP client | RestClient + Spring Retry |
| Mapping | MapStruct |
| API docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, AssertJ, Testcontainers, WireMock, REST Assured |
| Build | Maven |
| Container | Docker multi-stage + docker-compose |

## Быстрый старт

```bash
# 1. Поднять всё одной командой
docker compose up --build

# 2. Swagger UI
open http://localhost:8080/swagger-ui.html

# 3. Health
curl http://localhost:8080/actuator/health
```

Локально без Docker (нужен PostgreSQL на 5432):

```bash
export DB_HOST=localhost DB_USER=limits DB_PASSWORD=limits DB_NAME=limits
./mvnw spring-boot:run
```

## API

### A. Приём транзакций (внутренний)

`POST /api/v1/transactions`

```json
{
  "account_from": "0000000123",
  "account_to": "9999999999",
  "currency_shortname": "KZT",
  "sum": 10000.45,
  "expense_category": "product",
  "datetime": "2022-01-30T00:00:00+06:00"
}
```

### B. Клиентский API

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/v1/client/limits` | Установить новый лимит (datetime = now) |
| GET | `/api/v1/client/limits?account_from=...` | Список лимитов |
| GET | `/api/v1/client/transactions/exceeded?account_from=...` | Транзакции, превысившие лимит |

Ошибки валидации и бизнес-ошибки возвращаются в формате **ProblemDetail (RFC 9457)**.

## Логика лимитов

- Лимит хранится **раздельно** по категориям `product` / `service`.
- Если лимит не задан — используется **1000 USD**.
- Новый лимит **не обновляет** существующий — только добавляет новую запись.  
  `limit_datetime` всегда = текущее время (`Clock`), нельзя поставить в прошлое/будущее.
- Флаг `limit_exceeded` выставляется относительно **того лимита, который действовал на момент транзакции**.
- Границы месяца считаются в часовом поясе **Asia/Almaty** (UTC+6).
- Concurrent-запросы одного account+category сериализуются через **pessimistic write lock** на строке лимита.

## Курсы валют

- Источник по умолчанию: [Twelve Data](https://twelvedata.com) (`1day` close).
- Курсы кэшируются в таблице `exchange_rate`.
- При недоступности close используется `previous_close`.
- **Стратегия отказоустойчивости**: таймауты (connect 3 s / read 5 s) + retry (3 попытки, exponential backoff).  
  Если внешний API окончательно недоступен — берётся последний известный курс из БД.  
  Транзакция **никогда не теряется**.

Переменная окружения: `EXCHANGE_RATE_API_KEY`.

## Тесты

```bash
./mvnw verify
```

Обязательный сквозной тест: `EndToEndCase1Test`  
— поднимает приложение на RANDOM_PORT, PostgreSQL в Testcontainers, WireMock для курсов,  
воспроизводит «1 случай» из таблицы задания через HTTP.

## AI в проекте

### Каким AI пользовался
- Grok (xAI) — генерация каркаса, сущностей, сервисов, тестов, Dockerfile, SKILL.md.

### Что написано вручную / проверено
- Логика `limit_exceeded` и границы месяца (Asia/Almaty).
- Pessimistic locking + расчёт remaining.
- Стратегия fallback курсов.
- Сквозной тест Case 1.
- Все архитектурные решения (append-only limits, Clock bean).

### Где AI ошибся и как заметил
- Первая версия `sumUsdInPeriod` не исключала текущую транзакцию — поймал на unit-тесте remaining == 0.
- MapStruct expression для enum → string потребовал ручной правки.
- В e2e-тесте clock и WireMock port нужно было прокинуть через `@DynamicPropertySource` — AI предложил статику, исправил.

### Подключённый MCP
Файл `.cursor/mcp.json` — MCP-сервер PostgreSQL (`@modelcontextprotocol/server-postgres`).  
Позволяет агенту читать схему и данные локальной БД (read-only connection string).  
Пример задачи, где помог: проверка, что после e2e-теста в `transaction` лежат ровно две строки с `limit_exceeded = true`.

### Скилл
`.agents/skills/add-currency-provider/SKILL.md`  
— пошаговая инструкция «добавить нового провайдера курсов».  
Агент находит его по description и применяет, ссылаясь на реальные классы проекта.

### AGENTS.md
В корне — единый файл инструкций для Codex / Cursor / Copilot / Claude Code.

## Оценка трудоёмкости

Заявлено при ознакомлении с заданием: **18–22 часа** чистого времени  
(архитектура + код + тесты + Docker + AI-артефакты + README).

## Бонусы

- **Redis-кэш курсов** — слой поверх БД (`app.redis.enabled=true`, в docker-compose включён).
- **Virtual threads** — параллельная конвертация (`toUsdParallel`) и prefetch курсов.
- **CI** — `.github/workflows/ci.yml` (JDK 21 + `mvn verify`).
- **MCP tools** — `GET /mcp/tools`, `POST /mcp/tools/get_limits`, `POST /mcp/tools/get_exceeded_transactions` (поверх клиентского API).

## Структура репозитория

```
src/main/java/com/bank/limitservice/
  client/          # ExchangeRateClient
  config/          # Clock, properties
  controller/      # Transaction + Client
  dto/ mapper/
  entity/
  exception/
  repository/
  service/
src/main/resources/db/migration/
src/test/...
.agents/skills/
.cursor/mcp.json
Dockerfile
docker-compose.yml
AGENTS.md
```
