package com.echocampus.landmark.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.landmark.dto.LandmarkCreateRequest;
import com.echocampus.landmark.dto.LandmarkUpdateRequest;
import com.echocampus.landmark.vo.LandmarkAdminVO;
import com.echocampus.landmark.vo.LandmarkDetailVO;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.vo.Result;
import com.echocampus.landmark.service.LandmarkAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/landmarks")
@RequireRole
@Tag(name = "管理员地标", description = "提供校园地标的增删改查功能")
public class LandmarkAdminController {
    private final LandmarkAdminService landmarkAdminService;

    public LandmarkAdminController(LandmarkAdminService landmarkAdminService) {
        this.landmarkAdminService = landmarkAdminService;
    }

    @GetMapping("/")
    @Operation(summary = "地标列表", description = "分页获取地标列表，支持按分类、校区、关键词筛选")
    public Result<Page<LandmarkAdminVO>> listLandmarks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID campusId,
            @RequestParam(required = false) String keyword) {
        return Result.success(landmarkAdminService.listLandmarks(page, pageSize, categoryId, campusId, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取地标详情", description = "根据ID获取地标详情（含楼层列表）")
    public Result<LandmarkDetailVO> getLandmark(@PathVariable UUID id) {
        return Result.success(landmarkAdminService.getLandmark(id));
    }

    @PostMapping("/")
    @Operation(summary = "创建地标", description = "创建一个新的地标")
    public Result<UUID> createLandmark(@Valid @RequestBody LandmarkCreateRequest request) {
        UUID id = landmarkAdminService.createLandmark(request);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新地标", description = "更新地标信息")
    public Result<Void> updateLandmark(@PathVariable UUID id, @RequestBody LandmarkUpdateRequest request) {
        landmarkAdminService.updateLandmark(id, request);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地标", description = "删除地标及其关联的图片、楼层")
    public Result<Void> deleteLandmark(@PathVariable UUID id) {
        landmarkAdminService.deleteLandmark(id);
        return Result.success(null);
    }
}
