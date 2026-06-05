package com.echocampus.landmark.service;

import com.echocampus.landmark.dto.ImageConfirmRequest;
import com.echocampus.landmark.dto.ImagePresignRequest;
import com.echocampus.landmark.dto.ImagePresignResponse;
import com.echocampus.landmark.vo.BatchDeleteImagesResponse;
import com.echocampus.landmark.vo.ImagePageVO;

import java.util.List;
import java.util.UUID;

public interface LandmarkImageAdminService {
    ImagePresignResponse presignUpload(UUID landmarkId, ImagePresignRequest request);
    UUID confirmUpload(UUID landmarkId, ImageConfirmRequest request);
    void setCover(UUID landmarkId, UUID imageId);
    void setCuratedImages(UUID landmarkId, List<UUID> imageIds);
    ImagePageVO listImages(UUID landmarkId, int page, int pageSize);
    void deleteImage(UUID landmarkId, UUID imageId);
    BatchDeleteImagesResponse batchDeleteImages(UUID landmarkId, List<UUID> imageIds);
}
