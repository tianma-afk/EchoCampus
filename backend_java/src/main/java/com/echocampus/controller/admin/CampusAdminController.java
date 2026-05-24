package com.echocampus.controller.admin;

import com.echocampus.dto.CampusCreateRequest;
import com.echocampus.vo.Result;
import com.echocampus.service.admin.CampusAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

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
    public Result<UUID> createCampus(@RequestBody CampusCreateRequest request) {
        UUID id = campusAdminService.createCampus(request);
        return Result.success(id);
    }
}
