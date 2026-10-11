package com.bank.limitservice.controller;

import com.bank.limitservice.dto.LimitRequest;
import com.bank.limitservice.dto.LimitResponse;
import com.bank.limitservice.dto.TransactionResponse;
import com.bank.limitservice.service.LimitServiceApi;
import com.bank.limitservice.service.TransactionServiceApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/client")
@Validated
@Tag(name = "Client API", description = "External client endpoints: limits and exceeded transactions")
public class ClientController {

    private final LimitServiceApi limitService;
    private final TransactionServiceApi transactionService;

    public ClientController(LimitServiceApi limitService, TransactionServiceApi transactionService) {
        this.limitService = limitService;
        this.transactionService = transactionService;
    }

    @PostMapping("/limits")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Set a new monthly limit (datetime is set automatically to now)")
    public LimitResponse createLimit(@Valid @RequestBody LimitRequest request) {
        return limitService.createLimit(request);
    }

    @GetMapping("/limits")
    @Operation(summary = "List all limits for an account")
    public List<LimitResponse> getLimits(
            @RequestParam("account_from")
            @Pattern(regexp = "\\d{10}") String accountFrom) {
        return limitService.getLimits(accountFrom);
    }

    @GetMapping("/transactions/exceeded")
    @Operation(summary = "List transactions that exceeded the limit, with limit details")
    public List<TransactionResponse> getExceeded(
            @RequestParam("account_from")
            @Pattern(regexp = "\\d{10}") String accountFrom) {
        return transactionService.getExceeded(accountFrom);
    }
}
