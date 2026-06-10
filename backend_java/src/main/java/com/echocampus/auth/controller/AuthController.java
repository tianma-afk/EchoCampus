package com.echocampus.auth.controller;

import com.echocampus.auth.dto.LoginRequest;
import com.echocampus.auth.dto.RegisterRequest;
import com.echocampus.auth.dto.SendCodeRequest;
import com.echocampus.auth.service.AuthService;
import com.echocampus.auth.vo.LoginVO;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import static com.echocampus.shared.exception.ErrorCode.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "用户认证", description = "提供登录、注册、验证码等功能")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-code")
    @Operation(summary = "发送邮箱验证码", description = "向指定邮箱发送验证码，用于注册或登录")
    public Result<Map<String, Object>> sendCode(@Valid @RequestBody SendCodeRequest request) {
        log.info("[发送验证码] 请求邮箱: {}", request.getEmail());
        try {
            authService.sendCode(request);
            log.info("[发送验证码] 成功 -> {}", request.getEmail());
            return Result.success(SUCCESS, "验证码已发送", Map.of("expires_in", 300));
        } catch (BusinessException e) {
            log.warn("[发送验证码] 业务异常 -> {} | {}: {}", request.getEmail(), e.getCode(), e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("[发送验证码] 系统异常 -> {} | {}", request.getEmail(), e.getMessage(), e);
            return Result.failure(SYSTEM_ERROR, e.getMessage());
        }
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册/登录", description = "通过邮箱验证码完成注册或登录")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        log.info("[注册/登录] 请求邮箱: {}", request.getEmail());
        try {
            LoginVO result = authService.register(request);
            log.info("[注册/登录] 成功 -> {} | token前8位: {}...", request.getEmail(), result.getAccessToken().substring(0, Math.min(8, result.getAccessToken().length())));
            return Result.success(SUCCESS, "登录成功", result);
        } catch (RuntimeException e) {
            log.warn("[注册/登录] 失败 -> {} | {}", request.getEmail(), e.getMessage());
            return Result.failure(SYSTEM_ERROR, e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "密码登录", description = "通过邮箱和密码登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        log.info("[密码登录] 请求邮箱: {}", request.getEmail());
        try {
            LoginVO result = authService.login(request);
            log.info("[密码登录] 成功 -> {}", request.getEmail());
            return Result.success(SUCCESS, "登录成功", result);
        } catch (RuntimeException e) {
            log.warn("[密码登录] 失败 -> {} | {}", request.getEmail(), e.getMessage());
            return Result.failure(SYSTEM_ERROR, e.getMessage());
        }
    }
}
