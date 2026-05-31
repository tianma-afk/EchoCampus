package com.echocampus.controller.user;

import com.echocampus.dto.SearchCallbackRequest;
import com.echocampus.service.user.AlgorithmUserService;
import com.echocampus.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 移动端算法任务回调控制器
 * 接收 Python 算法服务的图像搜索任务回调
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal/callback")
@RequiredArgsConstructor
@Tag(name = "移动端算法回调", description = "接收 Python 算法服务的图像搜索任务回调")
public class CallbackUserController {

    private final AlgorithmUserService algorithmUserService;

    /**
     * 图像搜索任务回调接口
     * 
     * 功能说明：
     * 1. 接收 Python 算法服务完成的图像搜索结果
     * 2. 解析回调数据（任务状态、匹配结果列表）
     * 3. 调用 Service 层处理业务逻辑，更新任务状态
     * 
     * 使用场景：
     * - Python 算法服务完成图像搜索后主动调用此接口
     * - 传递搜索结果（匹配的图片 ID 和相似度分数）
     * - Java 端根据结果更新任务状态并保存搜索结果
     * 
     * @param taskId 任务 ID（URL 路径参数）
     * @param request 回调请求体，包含任务结果和匹配列表
     * @return 统一响应结果
     * 
     * 请求示例：
     * PUT /api/v1/internal/callback/search/550e8400-e29b-41d4-a716-446655440000
     * Content-Type: application/json
     * 
     * {
     *   "result": "SUCCESS",
     *   "matches": [
     *     { "imageId": "uuid1", "score": 0.95 },
     *     { "imageId": "uuid2", "score": 0.87 }
     *   ]
     * }
     * 
     * 响应示例（成功）：
     * {
     *   "code": 200,
     *   "message": "success",
     *   "data": null
     * }
     * 
     * 响应示例（失败）：
     * {
     *   "code": 400,
     *   "message": "任务结果不能为空",
     *   "data": null
     * }
     */
    @PutMapping("/search/{taskId}")
    @Operation(summary = "图像搜索任务回调", description = "Python 算法服务完成搜索后回调此接口，返回匹配结果")
    public Result<Void> searchCallBack(@PathVariable UUID taskId,
                                       @Valid @RequestBody SearchCallbackRequest request) {
        log.info("接收到图像搜索任务回调: taskId={}, result={}", taskId, request.getResult());
        
        // 打印匹配结果（如果存在）
        if (request.getMatches() != null && !request.getMatches().isEmpty()) {
            log.info("匹配结果数量: {}", request.getMatches().size());
            for (int i = 0; i < request.getMatches().size(); i++) {
                SearchCallbackRequest.MatchResult match = request.getMatches().get(i);
                log.info("  [{}] imageId={}, score={}", i + 1, match.getImageId(), match.getScore());
            }
        }

        try {
            // 调用 Service 层处理回调逻辑，更新任务状态
            algorithmUserService.updateSearchTaskStatus(taskId, request);
            
            log.info("图像搜索任务回调处理成功: taskId={}", taskId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("处理图像搜索任务回调失败: taskId={}", taskId, e);
            return Result.failure(500, "处理回调失败: " + e.getMessage());
        }
    }
}
