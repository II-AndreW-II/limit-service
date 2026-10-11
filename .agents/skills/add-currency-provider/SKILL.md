---
name: add-currency-provider
description: >
  Adds a new exchange-rate provider (e.g. AlphaVantage, OpenExchangeRates) to the limit-service.
  Use when the user asks to support another currency API, switch provider, or implement
  a second RateClient. Do NOT use for changing existing TwelveData client behaviour
  or for Redis caching.
---

# Add a new currency-rate provider

## When to apply
- User wants a new external source of FX rates.
- User mentions AlphaVantage, OpenExchangeRates, Fixer, etc.

## Steps

1. **Implement the interface**  
   Package: `com.bank.limitservice.client.impl`  
   Implement `com.bank.limitservice.client.ExchangeRateProvider`:
   ```java
   @Component
   @ConditionalOnProperty(name = "app.exchange-rate.provider", havingValue = "alphavantage")
   public class AlphaVantageExchangeRateClient implements ExchangeRateProvider {
       public Optional<RateQuote> fetchDailyClose(String symbol, LocalDate date) { ... }
       public String providerName() { return "alphavantage"; }
   }
   ```
   Use `RestClient` with timeouts from `ExchangeRateProperties`.  
   Annotate with `@Retryable` (same settings as TwelveData).

2. **Do not change** `ExchangeRateServiceImpl` — it already depends on the interface; Spring injects the active `@ConditionalOnProperty` bean.

3. **Extend properties** if the provider needs extra keys — add fields to `ExchangeRateProperties`; document in `application.yml`.

4. **Write a test** in `src/test/java/com/bank/limitservice/client/`.  
   Use WireMock; assert `ExchangeRateServiceApi.toUsd(...)` returns expected amount.

5. **Document** in README under “Exchange rate providers”.

## Commands
```bash
./mvnw -Dtest=YourNewClientTest test
./mvnw verify
```

## Real classes
- `com.bank.limitservice.client.ExchangeRateProvider` (interface)
- `com.bank.limitservice.client.impl.TwelveDataExchangeRateClient`
- `com.bank.limitservice.service.impl.ExchangeRateServiceImpl`
- `com.bank.limitservice.config.ExchangeRateProperties`
