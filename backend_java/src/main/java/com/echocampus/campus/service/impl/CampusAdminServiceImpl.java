package com.echocampus.campus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.campus.dto.CampusCreateRequest;
import com.echocampus.campus.dto.CampusUpdateRequest;
import com.echocampus.campus.entity.CampusEntity;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.campus.mapper.CampusMapper;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.campus.service.CampusAdminService;
import com.echocampus.campus.vo.CampusVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CampusAdminServiceImpl implements CampusAdminService {
    private final CampusMapper campusMapper;
    private final LandmarkMapper landmarkMapper;

    public CampusAdminServiceImpl(CampusMapper campusMapper, LandmarkMapper landmarkMapper) {
        this.campusMapper = campusMapper;
        this.landmarkMapper = landmarkMapper;
    }

    @Override
    public UUID createCampus(CampusCreateRequest request) {
        if (nameExists(request.getName(), request.getUniversityId(), null)) {
            throw new RuntimeException("该大学下已存在同名校区");
        }
        CampusEntity entity = new CampusEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(request.getName());
        entity.setUniversityId(request.getUniversityId());
        campusMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Page<CampusVO> listCampuses(int page, int pageSize, UUID universityId) {
        Page<CampusEntity> entityPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<CampusEntity> wrapper = new LambdaQueryWrapper<>();
        if (universityId != null) {
            wrapper.eq(CampusEntity::getUniversityId, universityId);
        }
        wrapper.orderByAsc(CampusEntity::getName);
        campusMapper.selectPage(entityPage, wrapper);

        Page<CampusVO> voPage = new Page<>(page, pageSize, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(e -> CampusVO.builder()
                        .id(e.getId())
                        .name(e.getName())
                        .universityId(e.getUniversityId())
                        .build())
                .toList());
        return voPage;
    }

    @Override
    public CampusVO getCampus(UUID id) {
        CampusEntity entity = campusMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("校区不存在");
        }
        return CampusVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .universityId(entity.getUniversityId())
                .build();
    }

    @Override
    public void updateCampus(UUID id, CampusUpdateRequest request) {
        CampusEntity entity = campusMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("校区不存在");
        }
        UUID effectiveUniversityId = request.getUniversityId() != null ? request.getUniversityId() : entity.getUniversityId();
        if (request.getName() != null && !request.getName().isBlank()) {
            if (nameExists(request.getName(), effectiveUniversityId, id)) {
                throw new RuntimeException("该大学下已存在同名校区");
            }
            entity.setName(request.getName());
        }
        if (request.getUniversityId() != null) {
            if (request.getName() == null || request.getName().isBlank()) {
                if (nameExists(entity.getName(), request.getUniversityId(), id)) {
                    throw new RuntimeException("该大学下已存在同名校区");
                }
            }
            entity.setUniversityId(request.getUniversityId());
        }
        campusMapper.updateById(entity);
    }

    @Override
    public void deleteCampus(UUID id) {
        CampusEntity entity = campusMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("校区不存在");
        }
        Long landmarkCount = landmarkMapper.selectCount(
                new LambdaQueryWrapper<LandmarkEntity>()
                        .eq(LandmarkEntity::getCampusId, id));
        if (landmarkCount > 0) {
            throw new RuntimeException("该校区下存在地标，请先删除地标");
        }
        campusMapper.deleteById(id);
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
                .toList();
    }

    private boolean nameExists(String name, UUID universityId, UUID excludeId) {
        LambdaQueryWrapper<CampusEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampusEntity::getName, name)
               .eq(CampusEntity::getUniversityId, universityId);
        if (excludeId != null) {
            wrapper.ne(CampusEntity::getId, excludeId);
        }
        return campusMapper.selectCount(wrapper) > 0;
    }
}
