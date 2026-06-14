package com.echocampus.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.admin.dto.AdminCreateRequest;
import com.echocampus.admin.dto.AdminUpdateRequest;
import com.echocampus.admin.service.AdminManageService;
import com.echocampus.admin.vo.AdminVO;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.context.AuthContext;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.echocampus.shared.exception.ErrorCode.SUCCESS;
import static com.echocampus.shared.exception.ErrorCode.SYSTEM_ERROR;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/admins")
@RequireRole(RoleEnum.SUPER_ADMIN)
@Tag(name = "管理员管理", description = "超级管理员对管理员的增删查改")
public class AdminManageController {

    private final AdminManageService adminManageService;

    public AdminManageController(AdminManageService adminManageService) {
        this.adminManageService = adminManageService;
    }

    @GetMapping("/")
    @Operation(summary = "管理员列表", description = "分页获取管理员列表，支持关键词搜索")
    public Result<Page<AdminVO>> listAdmins(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            return Result.success(adminManageService.listAdmins(page, pageSize, keyword));
        } catch (RuntimeException e) {
            log.error("[管理员列表] 异常 -> {}", e.getMessage(), e);
            return Result.failure(SYSTEM_ERROR, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "管理员详情", description = "根据ID获取管理员详情")
    public Result<AdminVO> getAdmin(@PathVariable UUID id) {
        try {
            return Result.success(adminManageService.getAdmin(id));
        } catch (BusinessException e) {
            log.warn("[管理员详情] 业务异常 -> {}: {}", id, e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("[管理员详情] 异常 -> {}", e.getMessage(), e);
            return Result.failure(SYSTEM_ERROR, e.getMessage());
        }
    }

    @PostMapping("/")
    @Operation(summary = "创建管理员", description = "创建一个新的管理员账号")
    public Result<UUID> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        try {
            UUID id = adminManageService.createAdmin(request);
            return Result.success(SUCCESS, "创建成功", id);
        } catch (BusinessException e) {
            log.warn("[创建管理员] 业务异常 -> {}: {}", request.getUsername(), e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("[创建管理员] 异常 -> {}", e.getMessage(), e);
            return Result.failure(SYSTEM_ERROR, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新管理员", description = "更新管理员信息（用户名、密码、邮箱、角色）")
    public Result<Void> updateAdmin(@PathVariable UUID id, @Valid @RequestBody AdminUpdateRequest request) {
        try {
            adminManageService.updateAdmin(id, request);
            return Result.success(null);
        } catch (BusinessException e) {
            log.warn("[更新管理员] 业务异常 -> id={}: {}", id, e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("[更新管理员] 异常 -> {}", e.getMessage(), e);
            return Result.failure(SYSTEM_ERROR, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除管理员", description = "删除管理员（不能删除自己，不能删除最后一个超级管理员）")
    public Result<Void> deleteAdmin(@PathVariable UUID id) {
        String operatorId = AuthContext.get().getUserId();
        try {
            adminManageService.deleteAdmin(id, operatorId);
            return Result.success(null);
        } catch (BusinessException e) {
            log.warn("[删除管理员] 业务异常 -> id={}: {}", id, e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("[删除管理员] 异常 -> {}", e.getMessage(), e);
            return Result.failure(SYSTEM_ERROR, e.getMessage());
        }
    }
}
