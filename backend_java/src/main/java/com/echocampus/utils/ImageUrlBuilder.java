package com.echocampus.utils;

import com.echocampus.config.MinioConfig;
import io.minio.http.Method;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class ImageUrlBuilder {

    private final MinioUtil minioUtil;
    private final String bucket;

    public ImageUrlBuilder(MinioConfig minioConfig, MinioUtil minioUtil) {
        this.bucket = minioConfig.getBucket();
        this.minioUtil = minioUtil;
    }

    public String buildUrl(UUID universityId, UUID campusId, UUID landmarkId,
                           UUID imageId, String fileExt) {
        String objectName = String.format("imgs/%s/%s/%s/%s.%s",
                universityId, campusId, landmarkId, imageId, fileExt);
        try {
            return minioUtil.getPresignedObjectUrl(
                    bucket, objectName, 5, TimeUnit.MINUTES, Method.GET, null);
        } catch (Exception e) {
            throw new RuntimeException("生成预签名URL失败: " + e.getMessage(), e);
        }
    }
}
