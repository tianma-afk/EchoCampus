package com.echocampus.client.impl;

import com.echocampus.client.AlgorithmClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AlgorithmClientImpl implements AlgorithmClient {


    @Override
    public String submitVectoredTask(List<Map<UUID,String>> imageUrl, String callbackUrl) {
        return "task-vector-123"; // TODO: 实现向算法后台发送向量化任务的请求 并 返回任务ID
    }

    @Override
    public String submitSearchTask(String imageUrl, String callbackUrl) {
        return "task-search-123"; // TODO: 实现向算法后台发送搜索任务的请求 并 返回任务ID
    }



}
