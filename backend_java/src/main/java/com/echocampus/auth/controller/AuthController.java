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
import org.springframework.web.bind.annotation.*;
import java.util.Map;

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
        try {
            authService.sendCode(request);
            return Result.success("验证码已发送", Map.of("expires_in", 300));
        } catch (BusinessException e) {
            return Result.failure(e.getCode(), e.getMessage());
        } catch (RuntimeException e) {
            return Result.failure(400, e.getMessage());
        }
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "通过邮箱验证码完成注册")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        try {
            LoginVO result = authService.register(request);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.failure(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过邮箱和密码或验证码登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginVO result = authService.login(request);
            return Result.success(result);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.contains("不存在") || msg.contains("错误")) {
                return Result.failure(401, msg);
            }
            return Result.failure(400, msg);
        }
    }
}
