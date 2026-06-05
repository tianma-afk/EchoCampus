package com.echocampus.algorithm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CallbackRequest {

    @Schema(description = "任务结果", example = "SUCCESS")
    @NotNull(message = "任务结果不能为空")
    private String result;
}
