package com.echocampus.algorithm.service.impl;

import com.echocampus.algorithm.client.AlgorithmClient;
import com.echocampus.algorithm.dto.SearchCallbackRequest;
import com.echocampus.campus.entity.CampusEntity;
import com.echocampus.landmark.entity.ImageEntity;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.algorithm.entity.TaskEntity;
import com.echocampus.shared.enums.TaskStatusEnum;
import com.echocampus.shared.enums.TaskTypeEnum;
import com.echocampus.campus.mapper.CampusMapper;
import com.echocampus.landmark.mapper.ImageMapper;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.algorithm.mapper.TaskMapper;
import com.echocampus.algorithm.service.AlgorithmUserService;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.util.CallBackUrlBuilder;
import com.echocampus.shared.util.ImageUrlBuilder;
import com.echocampus.algorithm.vo.SearchResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 移动端算法服务实现类
 * 处理移动端图像搜索等算法相关功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlgorithmUserServiceImpl implements AlgorithmUserService {

    private final AlgorithmClient algorithmClient;
    private final CallBackUrlBuilder callBackUrlBuilder;
    private final TaskMapper taskMapper;
    private final ImageMapper imageMapper;
    private final LandmarkMapper landmarkMapper;
    private final CampusMapper campusMapper;
    private final ImageUrlBuilder imageUrlBuilder;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建图像搜索任务
     * 
     * 实现逻辑（模仿管理端向量化任务）：
     * 1. 获取移动端用户上传的图片 URL（由前端传入）
     * 2. 创建 search 类型的任务记录到数据库，初始状态为 READY
     * 3. 构造回调 URL，格式：http://localhost:8080/api/v1/internal/callback/search/{taskId}
     * 4. 调用 AlgorithmClient.submitSearchTask 发送任务给 Python（暂不实现真正通信）
     * 5. 获得算法任务 ID，更新任务数据库表的 alg_task_id 字段
     * 
     * @param imageUrl 移动端用户上传到 MinIO 的图片 URL
     *                 来自移动端 UploadService 上传后的 URL
     *                 格式示例：http://localhost:9000/campus/images/2025/05/30/a1b2c3d4.jpg
     * @return 任务 ID（Java 端生成的 UUID，用于前端查询任务状态）
     */
    @Override
    public UUID createSearchTask(String imageUrl) {
        // 参数校验
        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "图片 URL 不能为空");
        }

        log.info("开始创建图像搜索任务: imageUrl={}", imageUrl);

        // 步骤 1：创建任务记录到数据库
        TaskEntity task = new TaskEntity();
        task.setId(UUID.randomUUID());
        task.setTaskType(TaskTypeEnum.SEARCH.getValue());  // 任务类型：SEARCH
        task.setTaskStatus(TaskStatusEnum.READY.getValue()); // 初始状态：READY
        taskMapper.insert(task);
        UUID taskId = task.getId();

        log.info("任务记录创建成功: taskId={}, taskType=SEARCH, status=READY", taskId);

        // 步骤 2：构造回调 URL
        // 格式：http://localhost:8080/api/v1/internal/callback/search/{taskId}
        String callbackUrl = callBackUrlBuilder.build(taskId.toString(), TaskTypeEnum.SEARCH);
        log.info("构造回调 URL: {}", callbackUrl);

        // 步骤 3：发送任务给 Python 算法服务（暂不实现真正通信）
        // 请求体示例：
        // {
        //   "imageUrl": "http://localhost:9000/campus/images/2025/05/30/a1b2c3d4.jpg",
        //   "topK": 10,
        //   "callbackUrl": "http://localhost:8080/api/v1/internal/callback/search/550e8400-e29b-41d4-a716-446655440000"
        // }
        String algTaskId = algorithmClient.submitSearchTask(imageUrl, callbackUrl);
        log.info("算法任务提交成功: algTaskId={}", algTaskId);

        // 步骤 4：更新任务数据库表，保存算法任务 ID
        task.setAlgTaskId(algTaskId);
        taskMapper.updateById(task);

        log.info("任务创建完成: taskId={}, algTaskId={}", taskId, algTaskId);

        return taskId;
    }

    /**
     * 更新图像搜索任务状态
     * 
     * 实现逻辑：
     * 1. 根据 taskId 查询任务记录
     * 2. 更新任务状态为 Python 返回的结果（SUCCESS/FAILED）
     * 3. 如果任务成功且有匹配结果，处理匹配结果链：
     *    match.imageId → image表 → landmarkId → 按landmark分组取最高相似度
     *    → landmark表 → coverImageId + name
     *    → campus表 → universityId
     *    → image表(coverImage) → fileExt
     *    → ImageUrlBuilder 构造封面URL
     * 4. 序列化结果为 JSON 存入 task.searchResult
     * 
     * @param taskId 任务 ID（URL 路径参数）
     * @param request Python 回调请求，包含任务状态和匹配结果
     */
    @Override
    public void updateSearchTaskStatus(UUID taskId, SearchCallbackRequest request) {
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return;
        }

        String result = request.getResult();
        task.setTaskStatus(result);

        if ("SUCCESS".equals(result) && request.getMatches() != null && !request.getMatches().isEmpty()) {
            List<SearchResultVO> searchResults = processMatches(request.getMatches());
            try {
                task.setSearchResult(objectMapper.writeValueAsString(searchResults));
            } catch (JsonProcessingException e) {
                log.error("序列化搜索结果失败: taskId={}", taskId, e);
            }
        }

        taskMapper.updateById(task);
        log.info("任务状态更新成功: taskId={}, status={}, results={}",
                taskId, result,
                request.getMatches() != null ? request.getMatches().size() : 0);
    }

    private List<SearchResultVO> processMatches(List<SearchCallbackRequest.MatchResult> matches) {
        Map<UUID, Double> landmarkScores = new LinkedHashMap<>();

        for (SearchCallbackRequest.MatchResult match : matches) {
            try {
                UUID imageId = UUID.fromString(match.getImageId());
                ImageEntity image = imageMapper.selectById(imageId);
                if (image == null || image.getLandmarkId() == null) {
                    log.warn("无法解析匹配图片: imageId={}", match.getImageId());
                    continue;
                }
                UUID landmarkId = image.getLandmarkId();
                Double existing = landmarkScores.get(landmarkId);
                if (existing == null || match.getScore() > existing) {
                    landmarkScores.put(landmarkId, match.getScore());
                }
            } catch (IllegalArgumentException e) {
                log.warn("无效的图片ID格式: {}", match.getImageId(), e);
            }
        }

        List<SearchResultVO> results = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : landmarkScores.entrySet()) {
            UUID landmarkId = entry.getKey();
            Double maxScore = entry.getValue();

            LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
            if (landmark == null || landmark.getCoverImageId() == null) {
                log.warn("地标信息不完整: landmarkId={}", landmarkId);
                continue;
            }

            ImageEntity coverImage = imageMapper.selectById(landmark.getCoverImageId());
            if (coverImage == null) {
                log.warn("封面图片不存在: coverImageId={}", landmark.getCoverImageId());
                continue;
            }

            UUID universityId = null;
            if (landmark.getCampusId() != null) {
                CampusEntity campus = campusMapper.selectById(landmark.getCampusId());
                if (campus != null) {
                    universityId = campus.getUniversityId();
                }
            }
            if (universityId == null) {
                log.warn("无法获取大学ID: landmarkId={}, campusId={}", landmarkId, landmark.getCampusId());
                continue;
            }

            String coverUrl = imageUrlBuilder.buildUrl(
                    universityId,
                    landmark.getCampusId(),
                    landmarkId,
                    coverImage.getId(),
                    coverImage.getFileExt()
            );

            results.add(new SearchResultVO(
                    landmarkId.toString(),
                    landmark.getName(),
                    maxScore,
                    coverUrl
            ));
        }

        log.info("匹配结果处理完成: 输入{}条, 输出{}条", matches.size(), results.size());
        return results;
    }

    @Override
    public List<SearchResultVO> getSearchResult(UUID taskId) {
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null || task.getSearchResult() == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(task.getSearchResult(),
                    new TypeReference<List<SearchResultVO>>() {});
        } catch (JsonProcessingException e) {
            log.error("反序列化搜索结果失败: taskId={}", taskId, e);
            return Collections.emptyList();
        }
    }
}
