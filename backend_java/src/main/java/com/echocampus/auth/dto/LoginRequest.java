package com.echocampus.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @Schema(description = "邮箱地址", example = "user@example.com")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "验证码（验证码登录时使用）", example = "123456")
    private String code;
}
