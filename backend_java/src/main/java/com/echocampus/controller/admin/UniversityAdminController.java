package com.echocampus.controller.admin;

import com.echocampus.dto.UniversityCreateRequest;
import com.echocampus.vo.Result;
import com.echocampus.service.admin.UniversityAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/universities")
@Tag(name = "管理员大学", description = "提供大学的增删改查功能")
public class UniversityAdminController {
    private final UniversityAdminService universityAdminService;

    public UniversityAdminController(UniversityAdminService universityAdminService) {
        this.universityAdminService = universityAdminService;
    }

    @PostMapping("/")
    @Operation(summary = "创建大学", description = "创建一个新的大学")
    public Result<UUID> createUniversity(@Valid @RequestBody UniversityCreateRequest request) {
        UUID id = universityAdminService.createUniversity(request);
        return Result.success(id);
    }
}
