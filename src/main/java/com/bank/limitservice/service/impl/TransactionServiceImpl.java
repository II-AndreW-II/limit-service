package com.bank.limitservice.service.impl;

import com.bank.limitservice.config.AppConfig;
import com.bank.limitservice.dto.TransactionRequest;
import com.bank.limitservice.dto.TransactionResponse;
import com.bank.limitservice.entity.ExpenseCategory;
import com.bank.limitservice.entity.MonthlyLimit;
import com.bank.limitservice.entity.Transaction;
import com.bank.limitservice.mapper.TransactionMapper;
import com.bank.limitservice.repository.TransactionRepository;
import com.bank.limitservice.service.ExchangeRateServiceApi;
import com.bank.limitservice.service.LimitServiceApi;
import com.bank.limitservice.service.TransactionServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionServiceApi {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final LimitServiceApi limitService;
    private final ExchangeRateServiceApi exchangeRateService;
    private final TransactionMapper transactionMapper;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  LimitServiceApi limitService,
                                  ExchangeRateServiceApi exchangeRateService,
                                  TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.limitService = limitService;
        this.exchangeRateService = exchangeRateService;
        this.transactionMapper = transactionMapper;
    }

    @Override
    @Transactional
    public TransactionResponse accept(TransactionRequest request) {
        ExpenseCategory category = ExpenseCategory.fromString(request.expenseCategory());
        OffsetDateTime txDt = request.datetime();
        LocalDate txDate = txDt.atZoneSameInstant(AppConfig.APP_ZONE).toLocalDate();

        BigDecimal sumUsd = exchangeRateService.toUsd(
                request.currencyShortname(), request.sum(), txDate);

        MonthlyLimit activeLimit = limitService.resolveActiveLimitForUpdate(
                request.accountFrom(), category, txDt);

        OffsetDateTime periodStart = activeLimit.getLimitDatetime();
        OffsetDateTime periodEnd = endOfMonth(txDt);
        if (periodStart.isBefore(startOfMonth(txDt))) {
            periodStart = startOfMonth(txDt);
        }

        BigDecimal alreadySpent = transactionRepository.sumUsdInPeriod(
                request.accountFrom(),
                category.name(),
                periodStart,
                periodEnd,
                null);

        BigDecimal remaining = activeLimit.getLimitSum().subtract(alreadySpent);
        boolean exceeded = sumUsd.compareTo(remaining) > 0;

        Transaction tx = new Transaction(
                request.accountFrom(),
                request.accountTo(),
                request.currencyShortname().toUpperCase(),
                request.sum(),
                sumUsd,
                category,
                txDt,
                exceeded,
                activeLimit.getId() != null ? activeLimit : null
        );

        Transaction saved = transactionRepository.save(tx);
        log.info("Transaction saved id={} account={} sumUsd={} exceeded={}",
                saved.getId(), saved.getAccountFrom(), sumUsd, exceeded);

        return transactionMapper.toExceededResponse(saved, activeLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getExceeded(String accountFrom) {
        return transactionRepository.findExceededByAccount(accountFrom).stream()
                .map(transactionMapper::toExceededResponse)
                .toList();
    }

    private OffsetDateTime startOfMonth(OffsetDateTime dt) {
        ZonedDateTime z = dt.atZoneSameInstant(AppConfig.APP_ZONE);
        YearMonth ym = YearMonth.from(z);
        return ym.atDay(1).atStartOfDay(AppConfig.APP_ZONE).toOffsetDateTime();
    }

    private OffsetDateTime endOfMonth(OffsetDateTime dt) {
        ZonedDateTime z = dt.atZoneSameInstant(AppConfig.APP_ZONE);
        YearMonth ym = YearMonth.from(z);
        return ym.atEndOfMonth().plusDays(1).atStartOfDay(AppConfig.APP_ZONE).toOffsetDateTime();
    }
}
