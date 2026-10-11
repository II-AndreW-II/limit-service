package com.bank.limitservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LimitResponse(
        Long id,
        @JsonProperty("account_from") String accountFrom,
        @JsonProperty("expense_category") String expenseCategory,
        @JsonProperty("limit_sum") BigDecimal limitSum,
        @JsonProperty("limit_datetime") OffsetDateTime limitDatetime,
        @JsonProperty("currency_shortname") String currencyShortname
) {
}
