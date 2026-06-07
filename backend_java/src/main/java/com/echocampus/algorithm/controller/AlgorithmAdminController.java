package com.echocampus.algorithm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.algorithm.entity.TaskEntity;
import com.echocampus.algorithm.service.AlgorithmAdminService;
import com.echocampus.shared.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/images")
@Tag(name = "管理员图像", description = "提供图片算法功能")
public class AlgorithmAdminController {

    private final AlgorithmAdminService algorithmAdminService;
    public AlgorithmAdminController(AlgorithmAdminService algorithmAdminService) {
        this.algorithmAdminService = algorithmAdminService;
    }

    @GetMapping("/vectorize/all")
    @Operation(summary = "向量化所有图片", description = "返回向量化任务ID")
    public Result<UUID> vectorizeAll() {
        UUID result = algorithmAdminService.createVectorTaskForAllImages();
        return Result.success(result);
    }

    @GetMapping("/tasks")
    @Operation(summary = "任务列表", description = "分页获取任务列表")
    public Result<Page<TaskEntity>> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(algorithmAdminService.listTasks(page, pageSize));
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "查询任务状态", description = "根据任务ID查询任务状态")
    public Result<TaskEntity> getTaskStatus(@PathVariable UUID id) {
        TaskEntity task = algorithmAdminService.getTaskStatus(id);
        return Result.success(task);
    }
}

