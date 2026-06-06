package com.echocampus.algorithm.client;


import java.util.Map;
import java.util.UUID;

public interface AlgorithmClient {
    String submitInsertTask(Map<UUID, String> imageUrls, String callbackUrl);

    String submitSearchTask(String imgUrl, String callbackUrl,int topK, boolean usePairSimilarity);
    String submitSearchTask(String imgUrl, String callbackUrl,int topK);
    String submitSearchTask(String imgUrl, String callbackUrl);

}
