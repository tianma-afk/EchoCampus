package com.echocampus.university.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.university.dto.UniversityCreateRequest;
import com.echocampus.university.dto.UniversityUpdateRequest;
import com.echocampus.university.entity.UniversityEntity;
import com.echocampus.campus.mapper.CampusMapper;
import com.echocampus.university.mapper.UniversityMapper;
import com.echocampus.university.service.UniversityAdminService;
import com.echocampus.university.vo.UniversityVO;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UniversityAdminServiceImpl implements UniversityAdminService {
    private final UniversityMapper universityMapper;
    private final CampusMapper campusMapper;

    public UniversityAdminServiceImpl(UniversityMapper universityMapper, CampusMapper campusMapper) {
        this.universityMapper = universityMapper;
        this.campusMapper = campusMapper;
    }

    @Override
    public UUID createUniversity(UniversityCreateRequest request) {
        if (nameExists(request.getName(), null)) {
            throw new BusinessException(ErrorCode.UNIVERSITY_NAME_CONFLICT);
        }
        UniversityEntity entity = new UniversityEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(request.getName());
        universityMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Page<UniversityVO> listUniversities(int page, int pageSize) {
        Page<UniversityEntity> entityPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<UniversityEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(UniversityEntity::getName);
        universityMapper.selectPage(entityPage, wrapper);

        Page<UniversityVO> voPage = new Page<>(page, pageSize, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(e -> UniversityVO.builder().id(e.getId()).name(e.getName()).build())
                .toList());
        return voPage;
    }

    @Override
    public UniversityVO getUniversity(UUID id) {
        UniversityEntity entity = universityMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.UNIVERSITY_NOT_FOUND);
        }
        return UniversityVO.builder().id(entity.getId()).name(entity.getName()).build();
    }

    @Override
    public void updateUniversity(UUID id, UniversityUpdateRequest request) {
        UniversityEntity entity = universityMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.UNIVERSITY_NOT_FOUND);
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            if (nameExists(request.getName(), id)) {
                throw new BusinessException(ErrorCode.UNIVERSITY_NAME_CONFLICT);
            }
            entity.setName(request.getName());
        }
        universityMapper.updateById(entity);
    }

    @Override
    public void deleteUniversity(UUID id) {
        UniversityEntity entity = universityMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.UNIVERSITY_NOT_FOUND);
        }
        Long campusCount = campusMapper.selectCount(
                new LambdaQueryWrapper<com.echocampus.campus.entity.CampusEntity>()
                        .eq(com.echocampus.campus.entity.CampusEntity::getUniversityId, id));
        if (campusCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该大学下存在校区，请先删除校区");
        }
        universityMapper.deleteById(id);
    }

    @Override
    public Page<UniversityVO> searchUniversities(String keyword, int page, int pageSize) {
        Page<UniversityEntity> entityPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<UniversityEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(UniversityEntity::getName, keyword);
        }
        wrapper.orderByAsc(UniversityEntity::getName);
        universityMapper.selectPage(entityPage, wrapper);

        Page<UniversityVO> voPage = new Page<>(page, pageSize, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(e -> UniversityVO.builder().id(e.getId()).name(e.getName()).build())
                .toList());
        return voPage;
    }

    private boolean nameExists(String name, UUID excludeId) {
        LambdaQueryWrapper<UniversityEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UniversityEntity::getName, name);
        if (excludeId != null) {
            wrapper.ne(UniversityEntity::getId, excludeId);
        }
        return universityMapper.selectCount(wrapper) > 0;
    }
}
