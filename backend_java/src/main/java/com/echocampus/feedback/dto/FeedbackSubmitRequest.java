package com.echocampus.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FeedbackSubmitRequest {

    @Schema(description = "地标ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "地标ID不能为空")
    private UUID landmarkId;

    @Schema(description = "反馈类型", example = "INFO_ERROR")
    @NotBlank(message = "反馈类型不能为空")
    private String feedbackType;

    @Schema(description = "反馈正文", example = "这座建筑的名字写错了")
    @NotBlank(message = "反馈正文不能为空")
    private String content;

    @Schema(description = "用户校正的地标名称")
    private String correctLandmarkName;

    @Schema(description = "反馈图片URL")
    private String uploadUrl;
}
