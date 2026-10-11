package com.bank.limitservice.service;

import com.bank.limitservice.dto.LimitRequest;
import com.bank.limitservice.dto.LimitResponse;
import com.bank.limitservice.entity.ExpenseCategory;
import com.bank.limitservice.entity.MonthlyLimit;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Public contract for monthly limit operations.
 */
public interface LimitServiceApi {

    LimitResponse createLimit(LimitRequest request);

    List<LimitResponse> getLimits(String accountFrom);

    MonthlyLimit resolveActiveLimit(String accountFrom, ExpenseCategory category, OffsetDateTime txDatetime);

    MonthlyLimit resolveActiveLimitForUpdate(String accountFrom, ExpenseCategory category, OffsetDateTime txDatetime);
}
