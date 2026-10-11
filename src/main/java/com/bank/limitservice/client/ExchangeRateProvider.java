package com.bank.limitservice.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Abstraction over external FX rate providers (Twelve Data, Alpha Vantage, …).
 * Implementations live in {@code client.impl}.
 */
public interface ExchangeRateProvider {

    /**
     * Fetches daily close for a currency pair (e.g. {@code "KZT/USD"}).
     * Returns empty on failure after retries — caller falls back to DB cache.
     */
    Optional<RateQuote> fetchDailyClose(String symbol, LocalDate date);

    /** Provider id used in logs and {@code exchange_rate.source}. */
    String providerName();

    record RateQuote(
            String symbol,
            LocalDate rateDate,
            BigDecimal close,
            BigDecimal previousClose
    ) {
        public BigDecimal effectiveRate() {
            return close != null ? close : previousClose;
        }
    }
}
