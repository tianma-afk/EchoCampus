package com.echocampus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ImagePresignResponse {

    @Schema(description = "MinIO 对象 key", example = "landmarks/abc123/def456.jpg")
    private String key;

    @Schema(description = "预签名上传 URL")
    private String presignedUrl;
}
