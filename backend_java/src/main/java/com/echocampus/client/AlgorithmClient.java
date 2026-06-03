package com.echocampus.client;


import java.util.Map;
import java.util.UUID;

public interface AlgorithmClient {
    String submitInsertTask(Map<UUID, String> imageUrls, String callbackUrl);

    String submitSearchTask(String imageUrl, String callbackUrl,int topK, boolean usePairVPR);
    String submitSearchTask(String imageUrl, String callbackUrl,int topK);
    String submitSearchTask(String imageUrl, String callbackUrl);

}
