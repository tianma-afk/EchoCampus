package com.echocampus.service.admin.impl;

import com.echocampus.dto.FloorCreateDTO;
import com.echocampus.dto.LandmarkCreateRequest;
import com.echocampus.entity.FloorEntity;
import com.echocampus.entity.LandmarkEntity;
import com.echocampus.mapper.FloorMapper;
import com.echocampus.mapper.LandmarkMapper;
import com.echocampus.service.admin.LandmarkAdminService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LandmarkAdminServiceImpl implements LandmarkAdminService {
    private final LandmarkMapper landmarkMapper;
    private final FloorMapper floorMapper;

    public LandmarkAdminServiceImpl(LandmarkMapper landmarkMapper, FloorMapper floorMapper) {
        this.landmarkMapper = landmarkMapper;
        this.floorMapper = floorMapper;
    }

    @Override
    public UUID createLandmark(LandmarkCreateRequest request) {
        LandmarkEntity entity = new LandmarkEntity();
        entity.setName(request.getName());
        entity.setRating(request.getRating());
        entity.setCheckInCount(request.getCheckInCount());
        entity.setOpenTime(request.getOpenTime());
        entity.setCategoryId(request.getCategoryId());
        entity.setTags(request.getTags());
        entity.setBuildYear(request.getBuildYear());
        entity.setOpenTimeDetail(request.getOpenTimeDetail());
        entity.setFloors(request.getFloors());
        entity.setLocation(request.getLocation());
        entity.setDescription(request.getDescription());
        entity.setCampusId(request.getCampusId());
        entity.setTotalFloors(request.getTotalFloors());
        entity.setRecommendRate(request.getRecommendRate());
        landmarkMapper.insert(entity);

        List<FloorCreateDTO> floorList = request.getFloorList();
        if (floorList != null && !floorList.isEmpty()) {
            for (FloorCreateDTO dto : floorList) {
                FloorEntity floor = new FloorEntity();
                floor.setLandmarkId(entity.getId());
                floor.setFloorNumber(dto.getFloorNumber());
                floor.setFloorName(dto.getFloorName());
                floor.setTags(dto.getTags());
                floorMapper.insert(floor);
            }
        }

        return entity.getId();
    }
}
