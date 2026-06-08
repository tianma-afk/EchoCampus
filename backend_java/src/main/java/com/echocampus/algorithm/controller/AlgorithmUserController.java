package com.echocampus.algorithm.controller;

import com.echocampus.algorithm.service.AlgorithmUserService;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.vo.Result;
import com.echocampus.algorithm.vo.SearchResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 移动端算法任务控制器
 * 提供图像搜索等算法相关功能的 API 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user/algorithm")
@RequiredArgsConstructor
@Tag(name = "移动端算法任务", description = "提供图像搜索等算法相关功能")
public class AlgorithmUserController {

    private final AlgorithmUserService algorithmUserService;

    /**
     * 创建图像搜索任务
     * 
     * 功能说明：
     * 1. 接收移动端用户上传到 MinIO 的图片 URL
     * 2. 创建 search 类型的任务记录
     * 3. 调用 Python 算法服务进行图像搜索（异步）
     * 4. 返回任务 ID，前端可通过该 ID 查询任务状态
     * 
     * 使用场景：
     * - 移动端用户拍摄照片后上传
     * - 从相册选择图片后上传
     * - 上传完成后立即发起图像搜索
     * 
     * @param request 搜索任务请求，包含 imageUrl 字段
     * @return 任务 ID（UUID），用于后续查询任务状态和搜索结果
     * 
     * 请求示例：
     * POST /api/v1/user/algorithm/search
     * Content-Type: application/json
     * 
     * {
     *   "imageUrl": "http://localhost:9000/campus/images/2025/05/30/a1b2c3d4.jpg"
     * }
     * 
     * 响应示例：
     * {
     *   "code": 200,
     *   "message": "success",
     *   "data": "550e8400-e29b-41d4-a716-446655440000"
     * }
     */
    @PostMapping("/search")
    @Operation(summary = "创建图像搜索任务", description = "移动端用户上传照片后，创建图搜任务并调用 Python 算法服务")
    public Result<UUID> createSearchTask(@RequestBody SearchTaskRequest request) {
        log.info("接收到图像搜索任务请求: imageUrl={}", request.getImageUrl());
        
        UUID taskId = algorithmUserService.createSearchTask(request.getImageUrl());
        log.info("图像搜索任务创建成功: taskId={}", taskId);
        return Result.success(taskId);
    }

    /**
     * 查询图像搜索结果
     * 
     * 功能说明：
     * 1. 根据 taskId 查询已处理完成的识别结果
     * 2. 返回按 landmark 分组后每组最高相似度的结果列表
     * 3. 每条结果包含：建筑名、最高相似度、建筑封面图 URL
     * 
     * @param taskId 任务 ID
     * @return 识别结果列表
     * 
     * 响应示例：
     * {
     *   "code": 200,
     *   "message": "success",
     *   "data": [
     *     { "landmarkName": "图书馆", "similarity": 0.95, "coverUrl": "http://..." },
     *     { "landmarkName": "教学楼A", "similarity": 0.87, "coverUrl": "http://..." }
     *   ]
     * }
     */
    @GetMapping("/search/{taskId}/result")
    @Operation(summary = "查询图像搜索结果", description = "查询已处理完成的图像搜索识别结果")
    public Result<List<SearchResultVO>> getSearchResult(@PathVariable UUID taskId) {
        log.info("查询图像搜索结果: taskId={}", taskId);
        try{
            List<SearchResultVO> results = algorithmUserService.getSearchResult(taskId);
            return Result.success(results);
        }catch (Exception e){
            return Result.failure(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 搜索任务请求 DTO
     */
    @lombok.Data
    public static class SearchTaskRequest {
        /**
         * 移动端用户上传到 MinIO 的图片 URL
         */
        private String imageUrl;
    }
}
