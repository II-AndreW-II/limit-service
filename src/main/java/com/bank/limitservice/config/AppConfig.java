package com.bank.limitservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(ExchangeRateProperties.class)
public class AppConfig {

    public static final ZoneId APP_ZONE = ZoneId.of("Asia/Almaty");

    @Bean
    public Clock clock() {
        return Clock.system(APP_ZONE);
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
