package com.echocampus.service.admin;

import com.echocampus.dto.LandmarkCreateRequest;

public interface LandmarkAdminService {
    Long createLandmark(LandmarkCreateRequest request);
}
