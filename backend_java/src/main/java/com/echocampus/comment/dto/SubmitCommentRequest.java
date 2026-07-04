package com.echocampus.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SubmitCommentRequest {

    @NotNull(message = "地标ID不能为空")
    @Schema(description = "所属地标ID")
    private UUID landmarkId;

    @Schema(description = "父评论ID，回复时填写")
    private UUID parentId;

    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容", example = "这栋楼很漂亮！")
    private String content;
}
