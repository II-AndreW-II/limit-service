package com.bank.limitservice.mapper;

import com.bank.limitservice.dto.TransactionResponse;
import com.bank.limitservice.entity.MonthlyLimit;
import com.bank.limitservice.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "accountFrom", source = "tx.accountFrom")
    @Mapping(target = "accountTo", source = "tx.accountTo")
    @Mapping(target = "currencyShortname", source = "tx.currencyShortname")
    @Mapping(target = "sum", source = "tx.sum")
    @Mapping(target = "datetime", source = "tx.datetime")
    @Mapping(target = "expenseCategory", expression = "java(tx.getExpenseCategory().toApiValue())")
    @Mapping(target = "limitSum", expression = "java(limit != null ? limit.getLimitSum() : null)")
    @Mapping(target = "limitDatetime", expression = "java(limit != null ? limit.getLimitDatetime() : null)")
    @Mapping(target = "limitCurrencyShortname", expression = "java(limit != null ? limit.getCurrencyShortname() : null)")
    TransactionResponse toExceededResponse(Transaction tx, MonthlyLimit limit);

    default TransactionResponse toExceededResponse(Transaction tx) {
        return toExceededResponse(tx, tx.getAppliedLimit());
    }
}
