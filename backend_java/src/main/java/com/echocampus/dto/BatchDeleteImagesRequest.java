package com.echocampus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BatchDeleteImagesRequest {

    @Schema(description = "待删除的图片 ID 列表")
    @NotEmpty(message = "图片列表不能为空")
    private List<UUID> imageIds;
}
