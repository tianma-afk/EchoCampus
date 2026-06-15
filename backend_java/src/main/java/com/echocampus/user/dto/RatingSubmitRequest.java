package com.echocampus.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "评分提交请求")
public class RatingSubmitRequest {

    @NotNull
    @Schema(description = "地标ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID landmarkId;

    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "评分（1-5）", example = "4.5")
    private BigDecimal rating;
}
