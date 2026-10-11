package com.bank.limitservice.service.impl;

import com.bank.limitservice.client.ExchangeRateProvider;
import com.bank.limitservice.entity.ExchangeRate;
import com.bank.limitservice.repository.ExchangeRateRepository;
import com.bank.limitservice.service.ExchangeRateServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FX conversion with layered cache: Redis → PostgreSQL → external API.
 * Parallel multi-currency conversion uses virtual threads (Java 21).
 */
@Service
public class ExchangeRateServiceImpl implements ExchangeRateServiceApi {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateServiceImpl.class);
    private static final BigDecimal DEFAULT_USD_RATE = BigDecimal.ONE;
    private static final Duration REDIS_TTL = Duration.ofHours(24);
    private static final String REDIS_KEY_PREFIX = "fx:";

    private final ExchangeRateRepository repository;
    private final ExchangeRateProvider provider;
    private final Clock clock;
    private final StringRedisTemplate redis; // may be null if Redis is disabled
    private final ConcurrentHashMap<String, Object> pairLocks = new ConcurrentHashMap<>();

    /** Virtual-thread executor for parallel rate fetches. */
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ExchangeRateServiceImpl(ExchangeRateRepository repository,
                                   ExchangeRateProvider provider,
                                   Clock clock,
                                   @Autowired(required = false) StringRedisTemplate redis) {
        this.repository = repository;
        this.provider = provider;
        this.clock = clock;
        this.redis = redis;
    }

    @Override
    @Transactional
    public BigDecimal toUsd(String currency, BigDecimal amount, LocalDate txDate) {
        if ("USD".equalsIgnoreCase(currency)) {
            return amount.setScale(4, RoundingMode.HALF_UP);
        }
        String pair = currency.toUpperCase() + "/USD";
        BigDecimal rate = resolveRate(pair, txDate);
        return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }

    @Override
    public Map<String, BigDecimal> toUsdParallel(Map<String, BigDecimal> amountsByCurrency, LocalDate txDate) {
        Map<String, CompletableFuture<BigDecimal>> futures = new HashMap<>();
        for (Map.Entry<String, BigDecimal> e : amountsByCurrency.entrySet()) {
            String currency = e.getKey();
            BigDecimal amount = e.getValue();
            futures.put(currency, CompletableFuture.supplyAsync(
                    () -> toUsd(currency, amount, txDate), virtualExecutor));
        }
        Map<String, BigDecimal> result = new HashMap<>();
        futures.forEach((currency, future) -> result.put(currency, future.join()));
        return result;
    }

    @Override
    public void prefetchRates(Set<String> currencyPairs, LocalDate date) {
        currencyPairs.forEach(pair ->
                CompletableFuture.runAsync(() -> resolveRate(pair, date), virtualExecutor));
    }

    private BigDecimal resolveRate(String pair, LocalDate date) {
        // 0. Redis
        BigDecimal fromRedis = getFromRedis(pair, date);
        if (fromRedis != null) {
            return fromRedis;
        }

        // 1. Exact date in DB
        Optional<ExchangeRate> cached = repository.findByCurrencyPairAndRateDate(pair, date);
        if (cached.isPresent()) {
            BigDecimal r = cached.get().effectiveRate();
            if (r != null) {
                putToRedis(pair, date, r);
                return r;
            }
        }

        // 2. Latest on or before date in DB
        Optional<ExchangeRate> latest = repository.findLatestOnOrBefore(pair, date);
        if (latest.isPresent() && latest.get().effectiveRate() != null) {
            BigDecimal r = latest.get().effectiveRate();
            putToRedis(pair, date, r);
            // refresh in background on virtual thread
            CompletableFuture.runAsync(() -> fetchAndStore(pair, date), virtualExecutor);
            return r;
        }

        // 3. External API under per-pair lock
        Object lock = pairLocks.computeIfAbsent(pair, k -> new Object());
        synchronized (lock) {
            fromRedis = getFromRedis(pair, date);
            if (fromRedis != null) {
                return fromRedis;
            }
            cached = repository.findByCurrencyPairAndRateDate(pair, date);
            if (cached.isPresent() && cached.get().effectiveRate() != null) {
                putToRedis(pair, date, cached.get().effectiveRate());
                return cached.get().effectiveRate();
            }
            latest = repository.findLatestOnOrBefore(pair, date);
            if (latest.isPresent() && latest.get().effectiveRate() != null) {
                putToRedis(pair, date, latest.get().effectiveRate());
                return latest.get().effectiveRate();
            }

            Optional<ExchangeRateProvider.RateQuote> quote = Optional.empty();
            try {
                quote = provider.fetchDailyClose(pair, date);
            } catch (Exception e) {
                log.warn("External rate fetch failed for {}: {}", pair, e.getMessage());
            }

            if (quote.isPresent() && quote.get().effectiveRate() != null) {
                ExchangeRateProvider.RateQuote q = quote.get();
                ExchangeRate er = new ExchangeRate(
                        pair, q.rateDate(), q.close(), q.previousClose(),
                        provider.providerName(), OffsetDateTime.now(clock));
                repository.save(er);
                putToRedis(pair, date, q.effectiveRate());
                return q.effectiveRate();
            }
        }

        log.error("No rate available for {} on or before {}. Using 1.0 as last resort.", pair, date);
        return DEFAULT_USD_RATE;
    }

    private void fetchAndStore(String pair, LocalDate date) {
        try {
            provider.fetchDailyClose(pair, date).ifPresent(q -> {
                if (repository.findByCurrencyPairAndRateDate(pair, q.rateDate()).isEmpty()) {
                    repository.save(new ExchangeRate(
                            pair, q.rateDate(), q.close(), q.previousClose(),
                            provider.providerName(), OffsetDateTime.now(clock)));
                    if (q.effectiveRate() != null) {
                        putToRedis(pair, date, q.effectiveRate());
                    }
                }
            });
        } catch (Exception e) {
            log.debug("Background rate refresh failed: {}", e.getMessage());
        }
    }

    private BigDecimal getFromRedis(String pair, LocalDate date) {
        if (redis == null) {
            return null;
        }
        try {
            String val = redis.opsForValue().get(redisKey(pair, date));
            return val != null ? new BigDecimal(val) : null;
        } catch (Exception e) {
            log.debug("Redis get failed: {}", e.getMessage());
            return null;
        }
    }

    private void putToRedis(String pair, LocalDate date, BigDecimal rate) {
        if (redis == null || rate == null) {
            return;
        }
        try {
            redis.opsForValue().set(redisKey(pair, date), rate.toPlainString(), REDIS_TTL);
        } catch (Exception e) {
            log.debug("Redis put failed: {}", e.getMessage());
        }
    }

    private static String redisKey(String pair, LocalDate date) {
        return REDIS_KEY_PREFIX + pair + ":" + date;
    }
}
