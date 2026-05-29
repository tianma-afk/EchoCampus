package com.echocampus.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.dto.LandmarkCreateRequest;
import com.echocampus.dto.LandmarkUpdateRequest;
import com.echocampus.vo.LandmarkAdminVO;
import com.echocampus.vo.LandmarkDetailVO;
import com.echocampus.vo.Result;
import com.echocampus.service.admin.LandmarkAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/landmarks")
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
        try {
            return Result.success(landmarkAdminService.getLandmark(id));
        } catch (RuntimeException e) {
            return Result.failure(404, e.getMessage());
        }
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
        try {
            landmarkAdminService.updateLandmark(id, request);
            return Result.success(null);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.contains("不存在")) {
                return Result.failure(404, msg);
            }
            return Result.failure(400, msg);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地标", description = "删除地标及其关联的图片、楼层")
    public Result<Void> deleteLandmark(@PathVariable UUID id) {
        try {
            landmarkAdminService.deleteLandmark(id);
            return Result.success(null);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.contains("不存在")) {
                return Result.failure(404, msg);
            }
            return Result.failure(400, msg);
        }
    }
}
