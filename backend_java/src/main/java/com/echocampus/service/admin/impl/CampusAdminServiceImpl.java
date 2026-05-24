package com.echocampus.service.admin.impl;

import com.echocampus.dto.CampusCreateRequest;
import com.echocampus.entity.CampusEntity;
import com.echocampus.mapper.CampusMapper;
import com.echocampus.service.admin.CampusAdminService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CampusAdminServiceImpl implements CampusAdminService {
    private final CampusMapper campusMapper;

    public CampusAdminServiceImpl(CampusMapper campusMapper) {
        this.campusMapper = campusMapper;
    }

    @Override
    public UUID createCampus(CampusCreateRequest request) {
        CampusEntity entity = new CampusEntity();
        entity.setName(request.getName());
        entity.setUniversityId(request.getUniversityId());
        campusMapper.insert(entity);
        return entity.getId();
    }
}
