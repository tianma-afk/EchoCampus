package com.echocampus.client;


import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AlgorithmClient {

    String submitVectoredTask(List<Map<UUID, String>> imageUrl, String callbackUrl);

    String submitSearchTask(String imageUrl, String callbackUrl);
    String submitSearchTask(String imageUrl, String callbackUrl, boolean usePairVPR);

}
