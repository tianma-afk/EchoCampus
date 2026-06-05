package com.echocampus.landmark.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CuratedImagesRequest {

    @Schema(description = "精选展示图片的 ID 列表（最多 5 张）")
    @NotEmpty(message = "图片列表不能为空")
    @Size(max = 5, message = "精选图片最多 5 张")
    private List<UUID> imageIds;
}
