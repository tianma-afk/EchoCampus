package com.echocampus.utils;

import com.echocampus.config.MinioConfig;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ImageUrlBuilder {

    private final String baseUrl;

    public ImageUrlBuilder(MinioConfig minioConfig) {
        this.baseUrl = minioConfig.getEndpoint() + "/" + minioConfig.getBucket();
    }

    public String buildUrl(UUID universityId, UUID campusId, UUID landmarkId,
                           UUID imageId, String fileExt) {
        return String.format("%s/imgs/%s/%s/%s/%s.%s",
                baseUrl, universityId, campusId, landmarkId, imageId, fileExt);
    }
}
