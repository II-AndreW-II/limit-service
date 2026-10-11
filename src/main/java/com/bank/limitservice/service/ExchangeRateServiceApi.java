package com.bank.limitservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Contract for FX conversion. Supports single and parallel multi-currency conversion.
 */
public interface ExchangeRateServiceApi {

    BigDecimal toUsd(String currency, BigDecimal amount, LocalDate txDate);

    /**
     * Converts several currencies to USD in parallel on virtual threads.
     * Key = currency code, value = amount in that currency → result map currency → USD amount.
     */
    Map<String, BigDecimal> toUsdParallel(Map<String, BigDecimal> amountsByCurrency, LocalDate txDate);

    /** Prefetch rates for the given pairs on virtual threads (fire-and-forget style). */
    void prefetchRates(Set<String> currencyPairs, LocalDate date);
}
