package com.echocampus.controller.admin;

import com.echocampus.dto.CampusCreateRequest;
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

    @PostMapping("/")
    @Operation(summary = "创建学院", description = "创建一个新的学院")
    public Result<UUID> createCampus(@Valid @RequestBody CampusCreateRequest request) {
        UUID id = campusAdminService.createCampus(request);
        return Result.success(id);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索校区", description = "根据关键词和大学ID模糊搜索校区")
    public Result<List<CampusVO>> searchCampuses(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) UUID universityId) {
        return Result.success(campusAdminService.searchCampuses(keyword, universityId));
    }
}
