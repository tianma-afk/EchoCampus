package com.echocampus.landmark.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImagePresignRequest {

    @Schema(description = "原始文件名，用于提取扩展名", example = "photo.jpg")
    @NotBlank(message = "文件名不能为空")
    private String filename;
}
