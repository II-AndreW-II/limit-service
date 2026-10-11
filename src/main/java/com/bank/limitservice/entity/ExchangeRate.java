package com.bank.limitservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exchange_rate", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pair_date", columnNames = {"currency_pair", "rate_date"})
})
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_pair", nullable = false, length = 10)
    private String currencyPair;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "close_rate", precision = 19, scale = 8)
    private BigDecimal closeRate;

    @Column(name = "previous_close", precision = 19, scale = 8)
    private BigDecimal previousClose;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    protected ExchangeRate() {
    }

    public ExchangeRate(String currencyPair, LocalDate rateDate, BigDecimal closeRate,
                        BigDecimal previousClose, String source, OffsetDateTime fetchedAt) {
        this.currencyPair = currencyPair;
        this.rateDate = rateDate;
        this.closeRate = closeRate;
        this.previousClose = previousClose;
        this.source = source;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCurrencyPair() {
        return currencyPair;
    }

    public LocalDate getRateDate() {
        return rateDate;
    }

    public BigDecimal getCloseRate() {
        return closeRate;
    }

    public BigDecimal getPreviousClose() {
        return previousClose;
    }

    public String getSource() {
        return source;
    }

    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }

    /**
     * Returns the rate to use for conversion: close if available, otherwise previous_close.
     */
    public BigDecimal effectiveRate() {
        if (closeRate != null) {
            return closeRate;
        }
        return previousClose;
    }
}
