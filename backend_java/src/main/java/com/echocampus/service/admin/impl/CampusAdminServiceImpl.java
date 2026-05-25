package com.echocampus.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.dto.CampusCreateRequest;
import com.echocampus.entity.CampusEntity;
import com.echocampus.mapper.CampusMapper;
import com.echocampus.service.admin.CampusAdminService;
import com.echocampus.vo.CampusVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Override
    public List<CampusVO> searchCampuses(String keyword, UUID universityId) {
        LambdaQueryWrapper<CampusEntity> wrapper = new LambdaQueryWrapper<>();
        if (universityId != null) {
            wrapper.eq(CampusEntity::getUniversityId, universityId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(CampusEntity::getName, keyword);
        }
        wrapper.last("LIMIT 20");
        List<CampusEntity> entities = campusMapper.selectList(wrapper);
        return entities.stream()
                .map(e -> CampusVO.builder()
                        .id(e.getId())
                        .name(e.getName())
                        .universityId(e.getUniversityId())
                        .build())
                .collect(Collectors.toList());
    }
}
