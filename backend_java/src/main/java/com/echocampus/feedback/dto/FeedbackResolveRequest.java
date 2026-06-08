package com.echocampus.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FeedbackResolveRequest {

    @Schema(description = "管理员ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "管理员ID不能为空")
    private UUID adminId;

    @Schema(description = "处理状态", example = "RESOLVED")
    @NotBlank(message = "处理状态不能为空")
    private String status;

    @Schema(description = "处理备注", example = "信息已核实并更新")
    private String resolveNote;
}
