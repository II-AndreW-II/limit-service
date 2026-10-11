package com.bank.limitservice.integration;

import com.bank.limitservice.entity.ExchangeRate;
import com.bank.limitservice.repository.ExchangeRateRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Сквозной API-автотест «1 случай» из таблицы задания.
 * Поднимает приложение на RANDOM_PORT, PostgreSQL в Testcontainers,
 * внешний API курсов замокан через WireMock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndCase1Test {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("limits")
            .withUsername("limits")
            .withPassword("limits");

    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @LocalServerPort
    int port;

    @Autowired
    ExchangeRateRepository rateRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("app.exchange-rate.base-url", () -> "http://localhost:" + wireMock.port());
        r.add("app.exchange-rate.api-key", () -> "test-key");
    }

    @TestConfiguration
    static class FixedClockConfig {
        static final AtomicReference<Instant> NOW = new AtomicReference<>(
                Instant.parse("2022-01-01T00:00:00Z"));

        @Bean
        @Primary
        Clock testClock() {
            return new Clock() {
                @Override public ZoneId getZone() { return ZoneId.of("Asia/Almaty"); }
                @Override public Clock withZone(ZoneId zone) { return this; }
                @Override public Instant instant() { return NOW.get(); }
            };
        }
    }

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
        // Always return a fixed close rate so conversion is deterministic (1 KZT = 0.002 USD → we use USD)
        wireMock.stubFor(get(urlPathEqualTo("/time_series"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "meta": {"symbol":"KZT/USD"},
                                  "values": [
                                    {"datetime":"2022-01-13","close":"0.0023"},
                                    {"datetime":"2022-01-12","close":"0.0023"}
                                  ]
                                }
                                """)));
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";
        // Seed rates for USD path (we send currency=USD in this test)
        LocalDate[] dates = {
                LocalDate.of(2022,1,1), LocalDate.of(2022,1,2), LocalDate.of(2022,1,3),
                LocalDate.of(2022,1,10), LocalDate.of(2022,1,11), LocalDate.of(2022,1,12),
                LocalDate.of(2022,1,13)
        };
        for (LocalDate d : dates) {
            if (rateRepository.findByCurrencyPairAndRateDate("USD/USD", d).isEmpty()) {
                rateRepository.save(new ExchangeRate("USD/USD", d, BigDecimal.ONE, BigDecimal.ONE,
                        "test", OffsetDateTime.of(d.atStartOfDay(), ZoneOffset.UTC)));
            }
        }
    }

    private void setClock(String iso) {
        FixedClockConfig.NOW.set(Instant.parse(iso));
    }

    private void postLimit(String account, String category, String sum) {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "account_from", account,
                        "expense_category", category,
                        "limit_sum", new BigDecimal(sum)
                ))
                .when()
                .post("/api/v1/client/limits")
                .then()
                .statusCode(201);
    }

    private void postTx(String account, String sum, String dt) {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "account_from", account,
                        "account_to", "9999999999",
                        "currency_shortname", "USD",
                        "sum", new BigDecimal(sum),
                        "expense_category", "product",
                        "datetime", dt
                ))
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(1)
    void case1_fullScenario() {
        String account = "0000000123";

        // 1.01.2022 limit 1000
        setClock("2022-01-01T00:00:00Z");
        postLimit(account, "product", "1000.00");

        // 2.01 500 → false
        setClock("2022-01-02T00:00:00Z");
        postTx(account, "500.00", "2022-01-02T00:00:00+06:00");

        // 3.01 600 → true
        setClock("2022-01-03T00:00:00Z");
        postTx(account, "600.00", "2022-01-03T00:00:00+06:00");

        // 10.01 new limit 2000
        setClock("2022-01-10T00:00:00Z");
        postLimit(account, "product", "2000.00");

        // 11.01 100 → false
        setClock("2022-01-11T00:00:00Z");
        postTx(account, "100.00", "2022-01-11T00:00:00+06:00");

        // 12.01 700 → false
        setClock("2022-01-12T00:00:00Z");
        postTx(account, "700.00", "2022-01-12T00:00:00+06:00");

        // 13.01 100 → false (spent 800, remaining 1200)
        setClock("2022-01-13T00:00:00Z");
        postTx(account, "100.00", "2022-01-13T00:00:00+06:00");

        // 13.01 1200 → true (exceeds remaining ~1100)
        postTx(account, "1200.00", "2022-01-13T12:00:00+06:00");

        // Assert exceeded list
        List<Map<String, Object>> exceeded = given()
                .queryParam("account_from", account)
                .when()
                .get("/api/v1/client/transactions/exceeded")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        assertThat(exceeded).hasSize(2);

        Map<String, Object> first = exceeded.get(0);
        assertThat(first.get("datetime").toString()).contains("2022-01-03");
        assertThat(new BigDecimal(first.get("limit_sum").toString())).isEqualByComparingTo("1000.00");
        assertThat(first.get("limit_currency_shortname")).isEqualTo("USD");

        Map<String, Object> second = exceeded.get(1);
        assertThat(second.get("datetime").toString()).contains("2022-01-13");
        assertThat(new BigDecimal(second.get("limit_sum").toString())).isEqualByComparingTo("2000.00");
        assertThat(second.get("limit_currency_shortname")).isEqualTo("USD");
    }
}
