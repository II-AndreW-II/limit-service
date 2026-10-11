package com.bank.limitservice.repository;

import com.bank.limitservice.entity.ExpenseCategory;
import com.bank.limitservice.entity.MonthlyLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MonthlyLimitRepository extends JpaRepository<MonthlyLimit, Long> {

    List<MonthlyLimit> findByAccountFromOrderByLimitDatetimeDesc(String accountFrom);

    /**
     * Finds the limit that was active at the given transaction datetime
     * (the latest limit whose limit_datetime <= transactionDatetime).
     * Uses pessimistic write lock to serialise concurrent transaction processing
     * for the same account+category.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ml FROM MonthlyLimit ml
            WHERE ml.accountFrom = :accountFrom
              AND ml.expenseCategory = :category
              AND ml.limitDatetime <= :txDatetime
            ORDER BY ml.limitDatetime DESC
            """)
    List<MonthlyLimit> findActiveLimitsForUpdate(
            @Param("accountFrom") String accountFrom,
            @Param("category") ExpenseCategory category,
            @Param("txDatetime") OffsetDateTime txDatetime);

    default Optional<MonthlyLimit> findActiveLimitForUpdate(
            String accountFrom, ExpenseCategory category, OffsetDateTime txDatetime) {
        List<MonthlyLimit> list = findActiveLimitsForUpdate(accountFrom, category, txDatetime);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Query("""
            SELECT ml FROM MonthlyLimit ml
            WHERE ml.accountFrom = :accountFrom
              AND ml.expenseCategory = :category
              AND ml.limitDatetime <= :txDatetime
            ORDER BY ml.limitDatetime DESC
            """)
    List<MonthlyLimit> findActiveLimits(
            @Param("accountFrom") String accountFrom,
            @Param("category") ExpenseCategory category,
            @Param("txDatetime") OffsetDateTime txDatetime);

    default Optional<MonthlyLimit> findActiveLimit(
            String accountFrom, ExpenseCategory category, OffsetDateTime txDatetime) {
        List<MonthlyLimit> list = findActiveLimits(accountFrom, category, txDatetime);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }
}

