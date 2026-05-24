package com.echocampus.service.admin.impl;

import com.echocampus.dto.LandmarkCreateRequest;
import com.echocampus.mapper.LandmarkMapper;
import com.echocampus.service.admin.LandmarkAdminService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LandmarkAdminServiceImpl implements LandmarkAdminService {
    private final LandmarkMapper landmarkMapper;

    public LandmarkAdminServiceImpl(LandmarkMapper landmarkMapper) {
        this.landmarkMapper = landmarkMapper;
    }

    @Override
    public UUID createLandmark(LandmarkCreateRequest request) {
//        return landmarkMapper.insertLandmark(request);
        return UUID.randomUUID();
    }

}
