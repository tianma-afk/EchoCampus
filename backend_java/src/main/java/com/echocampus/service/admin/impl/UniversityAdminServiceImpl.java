package com.echocampus.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.dto.UniversityCreateRequest;
import com.echocampus.entity.UniversityEntity;
import com.echocampus.mapper.UniversityMapper;
import com.echocampus.service.admin.UniversityAdminService;
import com.echocampus.vo.UniversityVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Override
    public List<UniversityVO> searchUniversities(String keyword) {
        LambdaQueryWrapper<UniversityEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(UniversityEntity::getName, keyword);
        }
        wrapper.last("LIMIT 20");
        List<UniversityEntity> entities = universityMapper.selectList(wrapper);
        return entities.stream()
                .map(e -> UniversityVO.builder()
                        .id(e.getId())
                        .name(e.getName())
                        .build())
                .collect(Collectors.toList());
    }
}
