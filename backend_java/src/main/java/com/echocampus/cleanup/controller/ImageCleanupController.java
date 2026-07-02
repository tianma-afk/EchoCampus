package com.echocampus.cleanup.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.cleanup.entity.MilvusPendingDeleteEntity;
import com.echocampus.cleanup.service.CleanupService;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.dto.BatchDeleteRequest;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.vo.Result;
import com.echocampus.algorithm.client.AlgorithmClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/cleanup")
@RequireRole
@Tag(name = "图片清理", description = "管理 Milvus 中残留的向量数据")
public class ImageCleanupController {

    private final CleanupService cleanupService;
    private final AlgorithmClient algorithmClient;

    public ImageCleanupController(CleanupService cleanupService, AlgorithmClient algorithmClient) {
        this.cleanupService = cleanupService;
        this.algorithmClient = algorithmClient;
    }

    @GetMapping("/pending-images")
    @Operation(summary = "获取待清理图片列表", description = "分页获取已删除但 Milvus 中仍有向量的图片 UUID")
    public Result<Page<MilvusPendingDeleteEntity>> listPending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(cleanupService.listPending(page, size));
    }

    @DeleteMapping("/pending-images")
    @Operation(summary = "批量删除 Milvus 向量", description = "将选中 UUID 发给 Python 删除 Milvus 向量，然后从待删队列移除")
    public Result<Void> batchDelete(@Valid @RequestBody BatchDeleteRequest request) {
        List<UUID> ids = request.getIds();
        if (ids.isEmpty()) return Result.success(null);

        List<MilvusPendingDeleteEntity> entities = cleanupService.listPending(1, ids.size()).getRecords();
        List<UUID> imageIds = entities.stream()
                .filter(e -> ids.contains(e.getId()))
                .map(MilvusPendingDeleteEntity::getImageId)
                .toList();

        if (imageIds.isEmpty()) return Result.success(null);

        // 调 Python 删除 Milvus 向量
        algorithmClient.submitDeleteTask(imageIds);

        // 从待删队列移除
        cleanupService.batchDelete(ids);
        log.info("[图片清理] 批量删除完成: pendingIds={}, imageIds={}", ids.size(), imageIds.size());
        return Result.success(ErrorCode.SUCCESS, "删除成功", null);
    }
}
