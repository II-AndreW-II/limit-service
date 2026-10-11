package com.bank.limitservice.mcp;

import com.bank.limitservice.dto.LimitResponse;
import com.bank.limitservice.dto.TransactionResponse;
import com.bank.limitservice.service.LimitServiceApi;
import com.bank.limitservice.service.TransactionServiceApi;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight MCP-style tool surface over the client API.
 * AI agents can call these endpoints as tools (list limits, exceeded transactions).
 * For full MCP protocol (stdio/SSE) wire this behind an MCP adapter or Spring AI MCP server.
 *
 * Tools:
 *  - get_limits(account_from)
 *  - get_exceeded_transactions(account_from)
 *  - list_tools
 */
@RestController
@RequestMapping("/mcp")
@Hidden
public class McpToolController {

    private final LimitServiceApi limitService;
    private final TransactionServiceApi transactionService;

    public McpToolController(LimitServiceApi limitService, TransactionServiceApi transactionService) {
        this.limitService = limitService;
        this.transactionService = transactionService;
    }

    @GetMapping(value = "/tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> listTools() {
        return Map.of(
                "tools", List.of(
                        Map.of(
                                "name", "get_limits",
                                "description", "List monthly expense limits for a bank account",
                                "parameters", Map.of(
                                        "account_from", Map.of("type", "string", "description", "10-digit account number")
                                )
                        ),
                        Map.of(
                                "name", "get_exceeded_transactions",
                                "description", "List transactions that exceeded the monthly limit, with limit details",
                                "parameters", Map.of(
                                        "account_from", Map.of("type", "string", "description", "10-digit account number")
                                )
                        )
                )
        );
    }

    @PostMapping(value = "/tools/get_limits", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getLimits(@RequestBody Map<String, String> args) {
        String account = args.get("account_from");
        List<LimitResponse> limits = limitService.getLimits(account);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool", "get_limits");
        result.put("account_from", account);
        result.put("data", limits);
        return result;
    }

    @PostMapping(value = "/tools/get_exceeded_transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getExceeded(@RequestBody Map<String, String> args) {
        String account = args.get("account_from");
        List<TransactionResponse> txs = transactionService.getExceeded(account);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool", "get_exceeded_transactions");
        result.put("account_from", account);
        result.put("data", txs);
        return result;
    }
}
