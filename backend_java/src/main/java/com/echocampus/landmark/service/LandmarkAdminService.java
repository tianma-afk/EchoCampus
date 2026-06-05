package com.echocampus.landmark.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.landmark.dto.LandmarkCreateRequest;
import com.echocampus.landmark.dto.LandmarkUpdateRequest;
import com.echocampus.landmark.vo.LandmarkAdminVO;
import com.echocampus.landmark.vo.LandmarkDetailVO;

import java.util.UUID;

public interface LandmarkAdminService {
    UUID createLandmark(LandmarkCreateRequest request);

    Page<LandmarkAdminVO> listLandmarks(int page, int pageSize, UUID categoryId, UUID campusId, String keyword);

    LandmarkDetailVO getLandmark(UUID id);

    void updateLandmark(UUID id, LandmarkUpdateRequest request);

    void deleteLandmark(UUID id);
}
