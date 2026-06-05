package com.echocampus.algorithm.client.impl;

import com.echocampus.algorithm.client.AlgorithmClient;
import com.echocampus.shared.util.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


@Service
public class AlgorithmClientImpl implements AlgorithmClient {
    private static final CircuitBreaker insertCB = new CircuitBreaker(10, 60000, 3, 0.5f);
    private static final CircuitBreaker serachCB = new CircuitBreaker(5, 30000, 5, 0.5f);
    public record ImageInfo(UUID uuid, String url) {}
    public record CreateInsertTaskRequest(List<ImageInfo> images, String callbackUrl) {}
    public record CreateSearchTaskRequest(String imageUrl, String callbackUrl,int TopK, boolean usePairVPR) {}

    static final String BASE_URL = "http://localhost:8000";

    private void sameIdCheck(String callbackUrl, String taskId){
        String uuidFromCallback = callbackUrl.substring(callbackUrl.lastIndexOf("/") + 1);
        String uuidFromTaskId = taskId.replace("alg-task-", "");
        if (!uuidFromCallback.equals(uuidFromTaskId))
            throw new RuntimeException("Callback URL does not match task ID");
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
                    .uri(URI.create(BASE_URL + "/insert"))
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
            throw new RuntimeException("Post insert task failed: " + e.getMessage());
        }
    }

    public String submitSearchTask(String imageUrl, String callbackUrl,int topK, boolean usePairVPR) {
        CreateSearchTaskRequest requestBody = new CreateSearchTaskRequest(imageUrl, callbackUrl, topK, usePairVPR);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String bodyJson = mapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/search"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = serachCB.execute(request);
            String taskId = response.body();

            sameIdCheck(callbackUrl, taskId);

            return taskId;
        }
        catch (Exception e){
            throw new RuntimeException("Post search task failed: ", e);
        }
    }
    public String submitSearchTask(String imageUrl, String callbackUrl,int topK) {
        return submitSearchTask(imageUrl, callbackUrl, topK,true);
    }

    public String submitSearchTask(String imageUrl, String callbackUrl) {
        return submitSearchTask(imageUrl, callbackUrl, 10);
    }
}
