package com.echocampus.auth.controller;

import com.echocampus.auth.dto.AdminLoginRequest;
import com.echocampus.auth.service.AdminAuthService;
import com.echocampus.auth.vo.LoginVO;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.echocampus.shared.exception.ErrorCode.SUCCESS;
import static com.echocampus.shared.exception.ErrorCode.SYSTEM_ERROR;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/auth")
@Tag(name = "管理员认证", description = "管理员登录")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "使用用户名和密码登录管理后台")
    public Result<LoginVO> login(@Valid @RequestBody AdminLoginRequest request) {
        log.info("[管理员登录] 请求 -> username={}", request.getUsername());
        try {
            LoginVO result = adminAuthService.login(request);
            log.info("[管理员登录] 成功 -> username={}", request.getUsername());
            return Result.success(SUCCESS, "登录成功", result);
        } catch (BusinessException e) {
            log.warn("[管理员登录] 业务异常 -> username={} | {}: {}",
                    request.getUsername(), e.getCode(), e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("[管理员登录] 系统异常 -> username={} | {}", request.getUsername(), e.getMessage(), e);
            return Result.failure(SYSTEM_ERROR, e.getMessage());
        }
    }
}
