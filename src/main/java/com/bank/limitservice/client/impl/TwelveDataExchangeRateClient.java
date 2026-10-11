package com.bank.limitservice.client.impl;

import com.bank.limitservice.client.ExchangeRateProvider;
import com.bank.limitservice.config.ExchangeRateProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Twelve Data implementation of {@link ExchangeRateProvider}.
 * Activated when {@code app.exchange-rate.provider=twelvedata} (default).
 */
@Component
@ConditionalOnProperty(name = "app.exchange-rate.provider", havingValue = "twelvedata", matchIfMissing = true)
public class TwelveDataExchangeRateClient implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(TwelveDataExchangeRateClient.class);

    private final RestClient restClient;
    private final ExchangeRateProperties props;

    public TwelveDataExchangeRateClient(RestClient.Builder builder, ExchangeRateProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeoutMs());
        factory.setReadTimeout(props.readTimeoutMs());
        this.restClient = builder
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public String providerName() {
        return "twelvedata";
    }

    @Override
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public Optional<RateQuote> fetchDailyClose(String symbol, LocalDate date) {
        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/time_series")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", "1day")
                            .queryParam("outputsize", 2)
                            .queryParam("apikey", props.apiKey())
                            .queryParam("start_date", date.minusDays(5).toString())
                            .queryParam("end_date", date.toString())
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null || !root.has("values") || root.get("values").isEmpty()) {
                log.warn("Empty response from Twelve Data for {}", symbol);
                return Optional.empty();
            }

            JsonNode values = root.get("values");
            BigDecimal close = null;
            BigDecimal previousClose = null;
            LocalDate rateDate = date;

            for (JsonNode v : values) {
                LocalDate d = LocalDate.parse(v.get("datetime").asText().substring(0, 10));
                BigDecimal c = new BigDecimal(v.get("close").asText());
                if (d.equals(date) || (close == null && !d.isAfter(date))) {
                    if (close == null) {
                        close = c;
                        rateDate = d;
                    } else if (previousClose == null) {
                        previousClose = c;
                    }
                }
            }

            if (close == null && !values.isEmpty()) {
                JsonNode latest = values.get(0);
                close = new BigDecimal(latest.get("close").asText());
                rateDate = LocalDate.parse(latest.get("datetime").asText().substring(0, 10));
            }
            if (previousClose == null && values.size() > 1) {
                previousClose = new BigDecimal(values.get(1).get("close").asText());
            }

            return Optional.of(new RateQuote(symbol, rateDate, close, previousClose));
        } catch (Exception e) {
            log.error("Failed to fetch rate for {}: {}", symbol, e.getMessage());
            throw e instanceof RestClientException rce ? rce : new RestClientException(e.getMessage(), e);
        }
    }
}
