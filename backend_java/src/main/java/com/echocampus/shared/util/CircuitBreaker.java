package com.echocampus.shared.util;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;
import java.time.Instant;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CircuitBreaker {
    enum State {CLOSED,OPEN,HALF_OPEN}
    private final long BASE_WAIT_MILLIS = 100L;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong openTime = new AtomicLong(0);
    private final AtomicInteger halfOpenCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenFinishCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenSuccessCount = new AtomicInteger(0);
    private final int failureThreshold;
    private final long timeoutMs;
    private final int halfOpenMaxRequests;
    private final int halfOpenSuccessThreshold;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))  // 设置连接超时为10秒
            .build();

    private void clearState(State stateToInit){
        switch (stateToInit){
            case CLOSED:
                failureCount.set(0);
                break;
            case OPEN:
                openTime.set(0);
                break;
            case HALF_OPEN:
                halfOpenCount.set(0);
                halfOpenSuccessCount.set(0);
                break;
        }
    }
    private int allowRequest(){     //放行机制
        int halfOpenCurrentCount = -1;
        switch(state.get()){
            case OPEN:
                if(openTime.get() + timeoutMs > Instant.now().toEpochMilli())
                    throw new RuntimeException("CircuitBreaker is open. Please wait for CircuitBreak closing");
                else
                    if(state.compareAndSet(State.OPEN, State.HALF_OPEN))
                        clearState(State.OPEN);
            case HALF_OPEN:
                halfOpenCurrentCount = halfOpenCount.incrementAndGet();
                if(halfOpenCurrentCount > halfOpenMaxRequests)
                    throw new RuntimeException("CircuitBreaker is halfOpen and request over than halfOpenMaxRequests");
                break;
        }
        return halfOpenCurrentCount;
    }

    private HttpResponse<String> tryPolicy(HttpRequest request) throws Exception {
        HttpResponse<String> response = null;
        int tryCount = 0;
        while (true) {
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                // 对于可重试的状态码，重试
                if ((statusCode == 429 || (statusCode >= 500 && statusCode < 600)) && tryCount < 5) {
                    Thread.sleep(BASE_WAIT_MILLIS * (long) Math.pow(2, tryCount));
                    tryCount++;
                    continue;
                }
                if (statusCode == 408 && tryCount < 1) {
                    Thread.sleep(BASE_WAIT_MILLIS);
                    tryCount++;
                    continue;
                }

                if (statusCode != 200 && statusCode != 201) {
                    throw new RuntimeException("Request failed with status code " + statusCode);
                }
                return response;

            } catch (IOException e) {
                if (tryCount < 2) {  // 超时重试 2 次
                    Thread.sleep(BASE_WAIT_MILLIS * (long) Math.pow(2, tryCount));
                    tryCount++;
                    continue;
                }
                throw e;  // 重试耗尽，抛出异常
            }
        }
    }

    private void failurePolicy(int halfOpenCurrentCount){
        switch (state.get()) {
            case CLOSED:
                failureCount.incrementAndGet();
                if (failureCount.get() >= failureThreshold)
                    if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                        clearState(State.CLOSED);
                        openTime.set(System.currentTimeMillis());
                    }
                break;
            case HALF_OPEN:
                halfOpenFinishCount.incrementAndGet();
                lastHalfOpenSuccessThresholdDetection();
        }
    }
    private void lastHalfOpenSuccessThresholdDetection(){
        if (halfOpenFinishCount.compareAndSet(halfOpenMaxRequests,-1)) {
            if(halfOpenSuccessCount.get() >= halfOpenSuccessThreshold){
                state.set(State.CLOSED);
            } else{
                state.set(State.OPEN);
            }
            clearState(State.HALF_OPEN);
        }
    }

    public CircuitBreaker(int failureThreshold, long timeoutMs, int halfOpenMaxRequests, int halfOpenSuccessThreshold){
        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeoutMs;
        this.halfOpenMaxRequests = halfOpenMaxRequests;
        this.halfOpenSuccessThreshold = halfOpenSuccessThreshold;
    }
    public CircuitBreaker(int failureThreshold, long timeoutMs, int halfOpenMaxRequests, float halfOpenSuccessRateThreshold) {
        this(failureThreshold, timeoutMs, halfOpenMaxRequests, (int)(halfOpenMaxRequests * halfOpenSuccessRateThreshold));
    }

    public HttpResponse<String> execute(HttpRequest request) throws Exception{
        int halfOpenCurrentCount = allowRequest();

        HttpResponse<String> response = null;

        try{
            response = tryPolicy(request);
        }
        catch (Exception e) {
            failurePolicy(halfOpenCurrentCount);
            throw e;
        }
        if(halfOpenCurrentCount != -1){
            halfOpenFinishCount.incrementAndGet();
            halfOpenSuccessCount.incrementAndGet();
            lastHalfOpenSuccessThresholdDetection();
        }
        return response;
    }
}
