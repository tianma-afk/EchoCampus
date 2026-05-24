package com.echocampus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CuratedImagesRequest {

    @Schema(description = "精选展示图片的 ID 列表（3-4 张）")
    @NotEmpty(message = "图片列表不能为空")
    @Size(max = 6, message = "精选图片最多 6 张")
    private List<UUID> imageIds;
}
