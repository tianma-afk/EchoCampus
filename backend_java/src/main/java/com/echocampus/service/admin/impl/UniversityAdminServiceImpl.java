package com.echocampus.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.dto.UniversityCreateRequest;
import com.echocampus.dto.UniversityUpdateRequest;
import com.echocampus.entity.UniversityEntity;
import com.echocampus.mapper.CampusMapper;
import com.echocampus.mapper.UniversityMapper;
import com.echocampus.service.admin.UniversityAdminService;
import com.echocampus.vo.UniversityVO;
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
            throw new RuntimeException("大学名称已存在");
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
            throw new RuntimeException("大学不存在");
        }
        return UniversityVO.builder().id(entity.getId()).name(entity.getName()).build();
    }

    @Override
    public void updateUniversity(UUID id, UniversityUpdateRequest request) {
        UniversityEntity entity = universityMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("大学不存在");
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            if (nameExists(request.getName(), id)) {
                throw new RuntimeException("大学名称已存在");
            }
            entity.setName(request.getName());
        }
        universityMapper.updateById(entity);
    }

    @Override
    public void deleteUniversity(UUID id) {
        UniversityEntity entity = universityMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("大学不存在");
        }
        Long campusCount = campusMapper.selectCount(
                new LambdaQueryWrapper<com.echocampus.entity.CampusEntity>()
                        .eq(com.echocampus.entity.CampusEntity::getUniversityId, id));
        if (campusCount > 0) {
            throw new RuntimeException("该大学下存在校区，请先删除校区");
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
