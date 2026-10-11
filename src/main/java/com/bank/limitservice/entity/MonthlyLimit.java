package com.bank.limitservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "monthly_limit", indexes = {
        @Index(name = "idx_limit_account_category", columnList = "account_from, expense_category, limit_datetime")
})
public class MonthlyLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_from", nullable = false, length = 10)
    private String accountFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false, length = 20)
    private ExpenseCategory expenseCategory;

    @Column(name = "limit_sum", nullable = false, precision = 19, scale = 2)
    private BigDecimal limitSum;

    @Column(name = "limit_datetime", nullable = false)
    private OffsetDateTime limitDatetime;

    @Column(name = "currency_shortname", nullable = false, length = 3)
    private String currencyShortname = "USD";

    @Version
    private Long version;

    protected MonthlyLimit() {
    }

    public MonthlyLimit(String accountFrom, ExpenseCategory expenseCategory,
                        BigDecimal limitSum, OffsetDateTime limitDatetime) {
        this.accountFrom = accountFrom;
        this.expenseCategory = expenseCategory;
        this.limitSum = limitSum;
        this.limitDatetime = limitDatetime;
        this.currencyShortname = "USD";
    }

    public Long getId() {
        return id;
    }

    public String getAccountFrom() {
        return accountFrom;
    }

    public ExpenseCategory getExpenseCategory() {
        return expenseCategory;
    }

    public BigDecimal getLimitSum() {
        return limitSum;
    }

    public OffsetDateTime getLimitDatetime() {
        return limitDatetime;
    }

    public String getCurrencyShortname() {
        return currencyShortname;
    }

    public Long getVersion() {
        return version;
    }
}
