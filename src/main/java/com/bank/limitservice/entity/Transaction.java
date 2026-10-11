package com.bank.limitservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transaction", indexes = {
        @Index(name = "idx_tx_account_category_dt", columnList = "account_from, expense_category, datetime"),
        @Index(name = "idx_tx_limit_exceeded", columnList = "account_from, limit_exceeded")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_from", nullable = false, length = 10)
    private String accountFrom;

    @Column(name = "account_to", nullable = false, length = 10)
    private String accountTo;

    @Column(name = "currency_shortname", nullable = false, length = 3)
    private String currencyShortname;

    @Column(name = "sum", nullable = false, precision = 19, scale = 2)
    private BigDecimal sum;

    @Column(name = "sum_usd", nullable = false, precision = 19, scale = 4)
    private BigDecimal sumUsd;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false, length = 20)
    private ExpenseCategory expenseCategory;

    @Column(name = "datetime", nullable = false)
    private OffsetDateTime datetime;

    @Column(name = "limit_exceeded", nullable = false)
    private boolean limitExceeded;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "limit_id")
    private MonthlyLimit appliedLimit;

    protected Transaction() {
    }

    public Transaction(String accountFrom, String accountTo, String currencyShortname,
                       BigDecimal sum, BigDecimal sumUsd, ExpenseCategory expenseCategory,
                       OffsetDateTime datetime, boolean limitExceeded, MonthlyLimit appliedLimit) {
        this.accountFrom = accountFrom;
        this.accountTo = accountTo;
        this.currencyShortname = currencyShortname;
        this.sum = sum;
        this.sumUsd = sumUsd;
        this.expenseCategory = expenseCategory;
        this.datetime = datetime;
        this.limitExceeded = limitExceeded;
        this.appliedLimit = appliedLimit;
    }

    public Long getId() {
        return id;
    }

    public String getAccountFrom() {
        return accountFrom;
    }

    public String getAccountTo() {
        return accountTo;
    }

    public String getCurrencyShortname() {
        return currencyShortname;
    }

    public BigDecimal getSum() {
        return sum;
    }

    public BigDecimal getSumUsd() {
        return sumUsd;
    }

    public ExpenseCategory getExpenseCategory() {
        return expenseCategory;
    }

    public OffsetDateTime getDatetime() {
        return datetime;
    }

    public boolean isLimitExceeded() {
        return limitExceeded;
    }

    public MonthlyLimit getAppliedLimit() {
        return appliedLimit;
    }
}
