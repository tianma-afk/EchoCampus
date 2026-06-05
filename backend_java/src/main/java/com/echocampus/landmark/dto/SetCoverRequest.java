package com.echocampus.landmark.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SetCoverRequest {

    @Schema(description = "封面图片的 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "图片 ID 不能为空")
    private UUID imageId;
}
