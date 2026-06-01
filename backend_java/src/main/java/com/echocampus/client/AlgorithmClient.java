package com.echocampus.client;


import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AlgorithmClient {
    // TODO: 解决HACK问题
    // HACK: [Lunaunde/2026-06-02] API设计问题：List<Map<UUID,String>> 存在冗余
    // 问题：
    // 1. Map<UUID,String> 本身已支持批量图片（key=图片ID, value=URL）
    // 2. 外层List导致歧义：是多个独立任务？还是支持分批处理？
    // 建议：
    // - 如果是单次批量任务，改为 Map<UUID,String>
    // - 如果必须支持多批次，需明确批次语义（如添加BatchInfo类）
    // - 目前是做一次请求处理
    String submitInsertTask(List<Map<UUID, String>> imageUrl, String callbackUrl);

    String submitSearchTask(String imageUrl, String callbackUrl,int topK, boolean usePairVPR);
    String submitSearchTask(String imageUrl, String callbackUrl,int topK);
    String submitSearchTask(String imageUrl, String callbackUrl);

}
