package com.bank.limitservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionRequest(
        @NotBlank
        @Pattern(regexp = "\\d{10}", message = "account_from must be exactly 10 digits")
        @JsonProperty("account_from")
        String accountFrom,

        @NotBlank
        @Pattern(regexp = "\\d{10}", message = "account_to must be exactly 10 digits")
        @JsonProperty("account_to")
        String accountTo,

        @NotBlank
        @Size(min = 3, max = 3)
        @JsonProperty("currency_shortname")
        String currencyShortname,

        @NotNull
        @DecimalMin(value = "0.01", message = "sum must be positive")
        @Digits(integer = 17, fraction = 2)
        BigDecimal sum,

        @NotBlank
        @Pattern(regexp = "product|service", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "expense_category must be 'product' or 'service'")
        @JsonProperty("expense_category")
        String expenseCategory,

        @NotNull
        OffsetDateTime datetime
) {
}
