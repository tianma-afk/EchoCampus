package com.echocampus.shared.config;

import com.echocampus.shared.util.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlgorithmClientConfig {

    @Bean(name = "insertCircuitBreaker")
    public CircuitBreaker insertCircuitBreaker(
            @Value("${algorithm.client.insert.failure-threshold:10}") int failureThreshold,
            @Value("${algorithm.client.insert.timeout-ms:60000}") long timeoutMs,
            @Value("${algorithm.client.insert.half-open-max-requests:3}") int halfOpenMaxRequests,
            @Value("${algorithm.client.insert.half-open-success-rate-threshold:0.5}") float halfOpenSuccessRateThreshold) {
        return new CircuitBreaker(failureThreshold, timeoutMs, halfOpenMaxRequests, halfOpenSuccessRateThreshold);
    }

    @Bean(name = "searchCircuitBreaker")
    public CircuitBreaker searchCircuitBreaker(
            @Value("${algorithm.client.search.failure-threshold:5}") int failureThreshold,
            @Value("${algorithm.client.search.timeout-ms:30000}") long timeoutMs,
            @Value("${algorithm.client.search.half-open-max-requests:5}") int halfOpenMaxRequests,
            @Value("${algorithm.client.search.half-open-success-rate-threshold:0.5}") float halfOpenSuccessRateThreshold) {
        return new CircuitBreaker(failureThreshold, timeoutMs, halfOpenMaxRequests, halfOpenSuccessRateThreshold);
    }
}