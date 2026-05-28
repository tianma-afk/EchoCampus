package com.echocampus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageConfirmRequest {

    @Schema(description = "上传完成后的 MinIO 对象 key", example = "imgs/uuid1/uuid2/uuid3/uuid4.jpg")
    @NotBlank(message = "key 不能为空")
    private String key;
}
