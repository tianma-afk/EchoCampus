package com.echocampus.shared.util;

import com.echocampus.shared.config.MinioConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ImageUrlBuilder {

    private final String bucket;
    private final String apiPublicBase;

    public ImageUrlBuilder(MinioConfig minioConfig,
                           @Value("${api.public-base:http://localhost:8080}") String apiPublicBase) {
        this.bucket = minioConfig.getBucket();
        this.apiPublicBase = apiPublicBase;
    }

    public String buildUrl(UUID universityId, UUID campusId, UUID landmarkId,
                           UUID imageId, String fileExt) {
        String objectName = String.format("imgs/%s/%s/%s/%s.%s",
                universityId, campusId, landmarkId, imageId, fileExt);
        return String.format("%s/api/v1/upload/files?bucket=%s&object=%s",
                apiPublicBase, bucket, objectName);
    }
}
