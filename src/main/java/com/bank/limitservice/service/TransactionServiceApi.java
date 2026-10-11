package com.bank.limitservice.service;

import com.bank.limitservice.dto.TransactionRequest;
import com.bank.limitservice.dto.TransactionResponse;

import java.util.List;

/**
 * Public contract for transaction acceptance and exceeded queries.
 */
public interface TransactionServiceApi {

    TransactionResponse accept(TransactionRequest request);

    List<TransactionResponse> getExceeded(String accountFrom);
}
