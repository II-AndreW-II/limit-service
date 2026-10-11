package com.bank.limitservice.controller;

import com.bank.limitservice.dto.TransactionRequest;
import com.bank.limitservice.dto.TransactionResponse;
import com.bank.limitservice.service.TransactionServiceApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions (internal)", description = "API for accepting expense transactions from banking systems")
public class TransactionController {

    private final TransactionServiceApi transactionService;

    public TransactionController(TransactionServiceApi transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Accept a new expense transaction")
    public TransactionResponse accept(@Valid @RequestBody TransactionRequest request) {
        return transactionService.accept(request);
    }
}
