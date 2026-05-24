package com.echocampus.service.admin;

import com.echocampus.dto.LandmarkCreateRequest;

import java.util.UUID;

public interface LandmarkAdminService {
    UUID createLandmark(LandmarkCreateRequest request);
}
