package com.echocampus.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "收藏切换请求")
public class FavoriteToggleRequest {

    @NotNull
    @Schema(description = "地标ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID landmarkId;
}
