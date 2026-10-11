package com.bank.limitservice.mapper;

import com.bank.limitservice.dto.LimitResponse;
import com.bank.limitservice.entity.MonthlyLimit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LimitMapper {

    @Mapping(target = "expenseCategory", expression = "java(limit.getExpenseCategory().toApiValue())")
    LimitResponse toResponse(MonthlyLimit limit);
}
