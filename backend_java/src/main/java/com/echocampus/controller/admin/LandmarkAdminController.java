package com.echocampus.controller.admin;

import com.echocampus.dto.LandmarkCreateRequest;
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

    @PostMapping("/")
    @Operation(summary = "创建地标", description = "创建一个新的地标")
    public Result<UUID> createLandmark(@Valid @RequestBody LandmarkCreateRequest request) {
        UUID id = landmarkAdminService.createLandmark(request);
        return Result.success(id);
    }




}
