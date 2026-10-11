package com.bank.limitservice.repository;

import com.bank.limitservice.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyPairAndRateDate(String currencyPair, LocalDate rateDate);

    /**
     * Latest rate on or before the given date (for previous_close fallback).
     */
    @Query("""
            SELECT er FROM ExchangeRate er
            WHERE er.currencyPair = :pair
              AND er.rateDate <= :date
            ORDER BY er.rateDate DESC
            """)
    List<ExchangeRate> findLatestOnOrBeforeList(
            @Param("pair") String pair,
            @Param("date") LocalDate date);

    default Optional<ExchangeRate> findLatestOnOrBefore(String pair, LocalDate date) {
        List<ExchangeRate> list = findLatestOnOrBeforeList(pair, date);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }
}

