package com.bank.limitservice.service.impl;

import com.bank.limitservice.config.AppConfig;
import com.bank.limitservice.dto.LimitRequest;
import com.bank.limitservice.dto.LimitResponse;
import com.bank.limitservice.entity.ExpenseCategory;
import com.bank.limitservice.entity.MonthlyLimit;
import com.bank.limitservice.mapper.LimitMapper;
import com.bank.limitservice.repository.MonthlyLimitRepository;
import com.bank.limitservice.service.LimitServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class LimitServiceImpl implements LimitServiceApi {

    public static final BigDecimal DEFAULT_LIMIT = new BigDecimal("1000.00");

    private final MonthlyLimitRepository limitRepository;
    private final LimitMapper limitMapper;
    private final Clock clock;

    public LimitServiceImpl(MonthlyLimitRepository limitRepository,
                            LimitMapper limitMapper,
                            Clock clock) {
        this.limitRepository = limitRepository;
        this.limitMapper = limitMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public LimitResponse createLimit(LimitRequest request) {
        ExpenseCategory category = ExpenseCategory.fromString(request.expenseCategory());
        OffsetDateTime now = OffsetDateTime.now(clock);

        MonthlyLimit limit = new MonthlyLimit(
                request.accountFrom(),
                category,
                request.limitSum(),
                now
        );
        return limitMapper.toResponse(limitRepository.save(limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LimitResponse> getLimits(String accountFrom) {
        return limitRepository.findByAccountFromOrderByLimitDatetimeDesc(accountFrom)
                .stream()
                .map(limitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyLimit resolveActiveLimit(String accountFrom, ExpenseCategory category,
                                           OffsetDateTime txDatetime) {
        return limitRepository.findActiveLimit(accountFrom, category, txDatetime)
                .orElseGet(() -> new MonthlyLimit(
                        accountFrom, category, DEFAULT_LIMIT,
                        OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0,
                                AppConfig.APP_ZONE.getRules().getOffset(java.time.Instant.EPOCH))
                ));
    }

    @Override
    @Transactional
    public MonthlyLimit resolveActiveLimitForUpdate(String accountFrom, ExpenseCategory category,
                                                    OffsetDateTime txDatetime) {
        return limitRepository.findActiveLimitForUpdate(accountFrom, category, txDatetime)
                .orElseGet(() -> {
                    MonthlyLimit def = new MonthlyLimit(
                            accountFrom, category, DEFAULT_LIMIT,
                            OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0,
                                    AppConfig.APP_ZONE.getRules().getOffset(java.time.Instant.EPOCH))
                    );
                    return limitRepository.save(def);
                });
    }
}
