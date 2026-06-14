package com.echocampus.university.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.university.dto.UniversityCreateRequest;
import com.echocampus.university.dto.UniversityUpdateRequest;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.vo.Result;
import com.echocampus.university.vo.UniversityVO;
import com.echocampus.university.service.UniversityAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/universities")
@RequireRole
@Tag(name = "管理员大学", description = "提供大学的增删改查功能")
public class UniversityAdminController {
    private final UniversityAdminService universityAdminService;

    public UniversityAdminController(UniversityAdminService universityAdminService) {
        this.universityAdminService = universityAdminService;
    }

    @GetMapping("/")
    @Operation(summary = "大学列表", description = "分页获取大学列表")
    public Result<Page<UniversityVO>> listUniversities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(universityAdminService.listUniversities(page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取大学详情", description = "根据ID获取单个大学")
    public Result<UniversityVO> getUniversity(@PathVariable UUID id) {
        return Result.success(universityAdminService.getUniversity(id));
    }

    @PostMapping("/")
    @Operation(summary = "创建大学", description = "创建一个新的大学")
    public Result<UUID> createUniversity(@Valid @RequestBody UniversityCreateRequest request) {
        UUID id = universityAdminService.createUniversity(request);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新大学", description = "更新大学名称")
    public Result<Void> updateUniversity(@PathVariable UUID id, @RequestBody UniversityUpdateRequest request) {
        universityAdminService.updateUniversity(id, request);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除大学", description = "删除大学（存在校区时拒绝）")
    public Result<Void> deleteUniversity(@PathVariable UUID id) {
        universityAdminService.deleteUniversity(id);
        return Result.success(null);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索大学", description = "根据关键词模糊搜索大学，支持分页")
    public Result<Page<UniversityVO>> searchUniversities(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(universityAdminService.searchUniversities(keyword, page, pageSize));
    }
}
