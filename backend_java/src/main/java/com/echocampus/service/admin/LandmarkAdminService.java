package com.echocampus.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.dto.LandmarkCreateRequest;
import com.echocampus.dto.LandmarkUpdateRequest;
import com.echocampus.vo.LandmarkAdminVO;
import com.echocampus.vo.LandmarkDetailVO;

import java.util.UUID;

public interface LandmarkAdminService {
    UUID createLandmark(LandmarkCreateRequest request);

    Page<LandmarkAdminVO> listLandmarks(int page, int pageSize, UUID categoryId, UUID campusId, String keyword);

    LandmarkDetailVO getLandmark(UUID id);

    void updateLandmark(UUID id, LandmarkUpdateRequest request);

    void deleteLandmark(UUID id);
}
