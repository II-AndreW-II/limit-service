package com.bank.limitservice.repository;

import com.bank.limitservice.entity.ExpenseCategory;
import com.bank.limitservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Sum of USD amounts for transactions in the limit period.
     * Native query with aggregate function as required by the assignment.
     */
    @Query(value = """
            SELECT COALESCE(SUM(t.sum_usd), 0)
            FROM transaction t
            WHERE t.account_from = :accountFrom
              AND t.expense_category = :category
              AND t.datetime >= :limitDatetime
              AND t.datetime < :periodEnd
              AND t.id <> COALESCE(:excludeId, -1)
            """, nativeQuery = true)
    BigDecimal sumUsdInPeriod(
            @Param("accountFrom") String accountFrom,
            @Param("category") String category,
            @Param("limitDatetime") OffsetDateTime limitDatetime,
            @Param("periodEnd") OffsetDateTime periodEnd,
            @Param("excludeId") Long excludeId);

    /**
     * Exceeded transactions with applied limit (JOIN + FETCH).
     * Satisfies the assignment requirement to use JOIN.
     */
    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.appliedLimit ml
            WHERE t.accountFrom = :accountFrom
              AND t.limitExceeded = true
            ORDER BY t.datetime
            """)
    List<Transaction> findExceededByAccount(@Param("accountFrom") String accountFrom);

    List<Transaction> findByAccountFromAndExpenseCategoryAndDatetimeGreaterThanEqualAndDatetimeLessThan(
            String accountFrom,
            ExpenseCategory category,
            OffsetDateTime from,
            OffsetDateTime to);
}
