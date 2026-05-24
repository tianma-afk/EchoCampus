package com.echocampus.service.admin.impl;

import com.echocampus.dto.UniversityCreateRequest;
import com.echocampus.entity.UniversityEntity;
import com.echocampus.mapper.UniversityMapper;
import com.echocampus.service.admin.UniversityAdminService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UniversityAdminServiceImpl implements UniversityAdminService {
    private final UniversityMapper universityMapper;

    public UniversityAdminServiceImpl(UniversityMapper universityMapper) {
        this.universityMapper = universityMapper;
    }

    @Override
    public UUID createUniversity(UniversityCreateRequest request) {
        UniversityEntity entity = new UniversityEntity();
        entity.setName(request.getName());
        universityMapper.insert(entity);
        return entity.getId();
    }
}
