package com.echocampus.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeedbackResolveRequest {

    @Schema(description = "处理状态", example = "RESOLVED")
    @NotBlank(message = "处理状态不能为空")
    private String status;

    @Schema(description = "处理备注", example = "信息已核实并更新")
    private String resolveNote;
}
