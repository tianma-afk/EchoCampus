package com.echocampus.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "管理员登录请求")
public class AdminLoginRequest {

    @Schema(description = "管理员邮箱", example = "admin@example.com")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @Schema(description = "密码", example = "admin123")
    @NotBlank(message = "密码不能为空")
    private String password;
}
