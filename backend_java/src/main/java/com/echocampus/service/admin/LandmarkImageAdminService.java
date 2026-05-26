package com.echocampus.service.admin;

import com.echocampus.dto.ImageConfirmRequest;
import com.echocampus.dto.ImagePresignRequest;
import com.echocampus.dto.ImagePresignResponse;
import com.echocampus.vo.LandmarkImageVO;

import java.util.List;
import java.util.UUID;

public interface LandmarkImageAdminService {
    ImagePresignResponse presignUpload(UUID landmarkId, ImagePresignRequest request);
    UUID confirmUpload(UUID landmarkId, ImageConfirmRequest request);
    void setCover(UUID landmarkId, UUID imageId);
    void setCuratedImages(UUID landmarkId, List<UUID> imageIds);
    List<LandmarkImageVO> listImages(UUID landmarkId);
    void deleteImage(UUID landmarkId, UUID imageId);
}
