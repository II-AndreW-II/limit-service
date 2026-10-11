package com.bank.limitservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(
        @JsonProperty("account_from") String accountFrom,
        @JsonProperty("account_to") String accountTo,
        @JsonProperty("currency_shortname") String currencyShortname,
        BigDecimal sum,
        @JsonProperty("expense_category") String expenseCategory,
        OffsetDateTime datetime,
        @JsonProperty("limit_sum") BigDecimal limitSum,
        @JsonProperty("limit_datetime") OffsetDateTime limitDatetime,
        @JsonProperty("limit_currency_shortname") String limitCurrencyShortname
) {
}
