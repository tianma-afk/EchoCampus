package com.echocampus.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新管理员请求")
public class AdminUpdateRequest {

    @Schema(description = "用户名", example = "admin2")
    @Size(min = 2, max = 64, message = "用户名长度为2-64位")
    private String username;

    @Schema(description = "密码（不填则不修改）", example = "654321")
    @Size(min = 6, max = 128, message = "密码长度为6-128位")
    private String password;

    @Schema(description = "邮箱", example = "admin2@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;
}
