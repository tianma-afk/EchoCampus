package com.echocampus.upload.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadPresignedUrlVO {
    private String uploadUrl;
    private String bucket;
    private String objectName;
}
