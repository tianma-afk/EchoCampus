package com.echocampus.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.dto.CampusCreateRequest;
import com.echocampus.dto.CampusUpdateRequest;
import com.echocampus.vo.CampusVO;
import com.echocampus.vo.Result;
import com.echocampus.service.admin.CampusAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/campuses")
@Tag(name = "管理员学院", description = "提供学院的增删改查功能")
public class CampusAdminController {
    private final CampusAdminService campusAdminService;

    public CampusAdminController(CampusAdminService campusAdminService) {
        this.campusAdminService = campusAdminService;
    }

    @GetMapping("/")
    @Operation(summary = "校区列表", description = "分页获取校区列表，可按大学ID筛选")
    public Result<Page<CampusVO>> listCampuses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) UUID universityId) {
        return Result.success(campusAdminService.listCampuses(page, pageSize, universityId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取校区详情", description = "根据ID获取单个校区")
    public Result<CampusVO> getCampus(@PathVariable UUID id) {
        try {
            return Result.success(campusAdminService.getCampus(id));
        } catch (RuntimeException e) {
            return Result.failure(404, e.getMessage());
        }
    }

    @PostMapping("/")
    @Operation(summary = "创建学院", description = "创建一个新的学院")
    public Result<UUID> createCampus(@Valid @RequestBody CampusCreateRequest request) {
        try {
            UUID id = campusAdminService.createCampus(request);
            return Result.success(id);
        } catch (RuntimeException e) {
            return Result.failure(409, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新校区", description = "更新校区名称或所属大学")
    public Result<Void> updateCampus(@PathVariable UUID id, @RequestBody CampusUpdateRequest request) {
        try {
            campusAdminService.updateCampus(id, request);
            return Result.success(null);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.contains("不存在")) {
                return Result.failure(404, msg);
            }
            return Result.failure(409, msg);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除校区", description = "删除校区（存在地标时拒绝）")
    public Result<Void> deleteCampus(@PathVariable UUID id) {
        try {
            campusAdminService.deleteCampus(id);
            return Result.success(null);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.contains("不存在")) {
                return Result.failure(404, msg);
            }
            return Result.failure(400, msg);
        }
    }

    @GetMapping("/search")
    @Operation(summary = "搜索校区", description = "根据关键词和大学ID模糊搜索校区")
    public Result<List<CampusVO>> searchCampuses(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) UUID universityId) {
        return Result.success(campusAdminService.searchCampuses(keyword, universityId));
    }
}
