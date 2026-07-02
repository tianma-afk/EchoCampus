package com.echocampus.algorithm.client.impl;

import com.echocampus.algorithm.client.AlgorithmClient;
import org.springframework.beans.factory.annotation.Value;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.exception.TechnicalException;
import com.echocampus.shared.util.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


@Slf4j
@Service
public class AlgorithmClientImpl implements AlgorithmClient {
    private final CircuitBreaker insertCB;
    private final CircuitBreaker searchCB;
    private final String baseUrl;

    public record ImageInfo(UUID uuid, String url) {}
    public record CreateInsertTaskRequest(List<ImageInfo> images, String callbackUrl) {}
    public record CreateSearchTaskRequest(String imgUrl, String callbackUrl,int topK, boolean usePairSimilarity) {}

    public AlgorithmClientImpl(@Qualifier("insertCircuitBreaker") CircuitBreaker insertCB,
                               @Qualifier("searchCircuitBreaker") CircuitBreaker searchCB,
                               @Value("${algorithm.client.base-url:http://localhost:8000}") String baseUrl) {
        this.insertCB = insertCB;
        this.searchCB = searchCB;
        this.baseUrl = baseUrl;
    }

    private void sameIdCheck(String callbackUrl, String taskId){
        String uuidFromCallback = callbackUrl.substring(callbackUrl.lastIndexOf("/") + 1);
        String uuidFromTaskId = taskId.replace("alg-task-", "");
        if (!uuidFromCallback.equals(uuidFromTaskId))
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Callback URL does not match task ID");
    }

    @Override
    public String submitInsertTask(Map<UUID, String> imageUrls, String callbackUrl) {

        List<ImageInfo> images = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : imageUrls.entrySet()) {
            images.add(new ImageInfo(entry.getKey(), entry.getValue()));
        }
        CreateInsertTaskRequest requestBody = new CreateInsertTaskRequest(images, callbackUrl);

        ObjectMapper mapper = new ObjectMapper();
        try {

            String bodyJson = mapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/insert"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();
            
            HttpResponse<String> response = insertCB.execute(request);
            String taskId = mapper.readTree(response.body()).get("taskId").asText();

            sameIdCheck(callbackUrl, taskId);

            return taskId;
        }
        catch (Exception e) {
            throw new TechnicalException(ErrorCode.ALGORITHM_SERVICE_ERROR, e);
        }
    }

    public String submitSearchTask(String imgUrl, String callbackUrl,int topK, boolean usePairSimilarity) {
        CreateSearchTaskRequest requestBody = new CreateSearchTaskRequest(imgUrl, callbackUrl, topK, usePairSimilarity);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String bodyJson = mapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/search"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = searchCB.execute(request);
            String taskId = mapper.readTree(response.body()).get("taskId").asText();

            sameIdCheck(callbackUrl, taskId);

            return taskId;
        }
        catch (Exception e){
            throw new TechnicalException(ErrorCode.ALGORITHM_SERVICE_ERROR, e);
        }
    }
    public String submitSearchTask(String imgUrl, String callbackUrl,int topK) {
        return submitSearchTask(imgUrl, callbackUrl, topK,true);
    }

    public String submitSearchTask(String imgUrl, String callbackUrl) {
        return submitSearchTask(imgUrl, callbackUrl, 10,true);
    }

    @Override
    public void submitDeleteTask(List<UUID> uuids) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String bodyJson = mapper.writeValueAsString(Map.of("uuids", uuids));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/delete"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();
            java.net.http.HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("调用 Python 删除 Milvus 向量失败", e);
        }
    }
}
