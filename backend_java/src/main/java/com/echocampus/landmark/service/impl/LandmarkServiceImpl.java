package com.echocampus.landmark.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.landmark.dto.LandmarkDetailRequest;
import com.echocampus.landmark.dto.LandmarkListRequest;
import com.echocampus.campus.entity.CampusEntity;
import com.echocampus.category.entity.CategoryEntity;
import com.echocampus.landmark.entity.FloorEntity;
import com.echocampus.landmark.entity.ImageEntity;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.university.entity.UniversityEntity;
import com.echocampus.shared.enums.CategoryEnum;
import com.echocampus.campus.mapper.CampusMapper;
import com.echocampus.category.mapper.CategoryMapper;
import com.echocampus.landmark.mapper.FloorMapper;
import com.echocampus.landmark.mapper.ImageMapper;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.university.mapper.UniversityMapper;
import com.echocampus.landmark.service.LandmarkService;
import com.echocampus.shared.util.ImageUrlBuilder;
import com.echocampus.landmark.vo.FloorVO;
import com.echocampus.landmark.vo.LandmarkDetailVO;
import com.echocampus.landmark.vo.LandmarkVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LandmarkServiceImpl implements LandmarkService {
    private final LandmarkMapper landmarkMapper;
    private final CategoryMapper categoryMapper;
    private final FloorMapper floorMapper;
    private final CampusMapper campusMapper;
    private final UniversityMapper universityMapper;
    private final ImageMapper imageMapper;
    private final ImageUrlBuilder imageUrlBuilder;

    public LandmarkServiceImpl(LandmarkMapper landmarkMapper, CategoryMapper categoryMapper,
                               FloorMapper floorMapper, CampusMapper campusMapper,
                               UniversityMapper universityMapper, ImageMapper imageMapper,
                               ImageUrlBuilder imageUrlBuilder) {
        this.landmarkMapper = landmarkMapper;
        this.categoryMapper = categoryMapper;
        this.floorMapper = floorMapper;
        this.campusMapper = campusMapper;
        this.universityMapper = universityMapper;
        this.imageMapper = imageMapper;
        this.imageUrlBuilder = imageUrlBuilder;
    }

    @Override
    public Page<LandmarkVO> getLandmarkList(LandmarkListRequest request) {
        LambdaQueryWrapper<LandmarkEntity> wrapper = new LambdaQueryWrapper<>();

        if (request.getCategory() != null) {
            String categoryName = CategoryEnum.getDbNameByCode(request.getCategory());
            if (categoryName != null) {
                CategoryEntity categoryEntity = categoryMapper.selectOne(
                        new LambdaQueryWrapper<CategoryEntity>().eq(CategoryEntity::getName, categoryName));
                if (categoryEntity != null) {
                    wrapper.eq(LandmarkEntity::getCategoryId, categoryEntity.getId());
                }
            }
        }

        if (StringUtils.isNotBlank(request.getSearchQuery())) {
            wrapper.like(LandmarkEntity::getName, request.getSearchQuery());
        }

		// 排序
		String sortBy = request.getSortBy();
		if ("rate".equals(sortBy)) {
			wrapper.orderByDesc(LandmarkEntity::getRating);
		} else if ("hot".equals(sortBy)) {
			wrapper.orderByDesc(LandmarkEntity::getCheckInCount);
		} else if ("nameAsc".equals(sortBy)) {
			wrapper.last(" ORDER BY convert_to(name, 'GBK') ASC");
		} else {
			wrapper.orderByAsc(LandmarkEntity::getId);
		}

        Page<LandmarkEntity> page = new Page<>(request.getPage(), request.getPageSize());
        Page<LandmarkEntity> resultPage = landmarkMapper.selectPage(page, wrapper);

        // 批量查询分类名称
        List<UUID> categoryIds = resultPage.getRecords().stream()
                .map(LandmarkEntity::getCategoryId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, String> categoryNameMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(CategoryEntity::getId, CategoryEntity::getName));

        // 批量查询封面图片
        List<UUID> coverImageIds = resultPage.getRecords().stream()
                .map(LandmarkEntity::getCoverImageId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, ImageEntity> coverImageMap = coverImageIds.isEmpty() ? Map.of()
                : imageMapper.selectBatchIds(coverImageIds).stream()
                        .collect(Collectors.toMap(ImageEntity::getId, img -> img));

        // 批量查询 campus → university，用于构造封面 URL
        List<UUID> campusIds = resultPage.getRecords().stream()
                .map(LandmarkEntity::getCampusId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, CampusEntity> campusMap = campusIds.isEmpty() ? Map.of()
                : campusMapper.selectBatchIds(campusIds).stream()
                        .collect(Collectors.toMap(CampusEntity::getId, c -> c));
        Map<UUID, UniversityEntity> universityMap;
        if (!campusMap.isEmpty()) {
            List<UUID> universityIds = campusMap.values().stream()
                    .map(CampusEntity::getUniversityId)
                    .distinct()
                    .collect(Collectors.toList());
            universityMap = universityMapper.selectBatchIds(universityIds).stream()
                    .collect(Collectors.toMap(UniversityEntity::getId, u -> u));
        } else {
            universityMap = Map.of();
        }

        List<LandmarkVO> voList = resultPage.getRecords().stream()
                .map(entity -> LandmarkVO.builder()
                        .id(entity.getId())
                        .name(entity.getName())
                        .rating(entity.getRating())
                        .checkins(entity.getCheckInCount())
                        .openTime(entity.getOpenTime())
                        .category(categoryNameMap.getOrDefault(entity.getCategoryId(), "未分类"))
                        .tags(entity.getTags())
                        .coverImg(buildCoverUrl(entity, coverImageMap, campusMap, universityMap))
                        .latitude(entity.getLatitude())
                        .longitude(entity.getLongitude())
                        .build())
                .collect(Collectors.toList());

        Page<LandmarkVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public LandmarkDetailVO getLandmarkDetail(LandmarkDetailRequest request) {
        LandmarkEntity landmark = landmarkMapper.selectById(request.getId());
        if (landmark == null) {
            return null;
        }

        CategoryEntity category = categoryMapper.selectById(landmark.getCategoryId());
        String categoryName = category != null ? category.getName() : "未分类";
        CampusEntity campus = campusMapper.selectById(landmark.getCampusId());
        String campusName = campus != null ? campus.getName() : null;
        UUID universityId = campus != null ? campus.getUniversityId() : null;
        String universityName = null;
        if (universityId != null) {
            UniversityEntity university = universityMapper.selectById(universityId);
            universityName = university != null ? university.getName() : null;
        }

        // 精选图片 UUID → URL
        List<String> imgUrls = List.of();
        if (landmark.getImgs() != null && !landmark.getImgs().isEmpty()) {
            List<ImageEntity> images = imageMapper.selectBatchIds(landmark.getImgs());
            imgUrls = images.stream()
                    .map(img -> imageUrlBuilder.buildUrl(
                            universityId, landmark.getCampusId(), landmark.getId(),
                            img.getId(), img.getFileExt()))
                    .collect(Collectors.toList());
        }

        List<FloorVO> floorList = floorMapper.selectList(
                new LambdaQueryWrapper<FloorEntity>().eq(FloorEntity::getLandmarkId, landmark.getId())
        ).stream().map(floor -> FloorVO.builder()
                .floorNumber(floor.getFloorNumber())
                .floorName(floor.getFloorName())
                .tags(floor.getTags())
                .build()
        ).collect(Collectors.toList());

        return LandmarkDetailVO.builder()
                .id(landmark.getId())
                .name(landmark.getName())
                .rating(landmark.getRating())
                .checkins(landmark.getCheckInCount())
                .openTime(landmark.getOpenTime())
                .category(categoryName)
                .tags(landmark.getTags())
                .imgs(imgUrls)
                .buildYear(landmark.getBuildYear())
                .openTimeDetail(landmark.getOpenTimeDetail())
                .floors(landmark.getFloors())
                .location(landmark.getLocation())
                .latitude(landmark.getLatitude())
                .longitude(landmark.getLongitude())
                .description(landmark.getDescription())
                .campusName(campusName)
                .universityName(universityName)
                .totalFloors(landmark.getTotalFloors())
                .recommendRate(landmark.getRecommendRate())
                .floorList(floorList)
                .build();
    }

    private String buildCoverUrl(LandmarkEntity entity, Map<UUID, ImageEntity> coverImageMap,
                                  Map<UUID, CampusEntity> campusMap, Map<UUID, UniversityEntity> universityMap) {
        UUID coverImageId = entity.getCoverImageId();
        if (coverImageId == null) {
            return null;
        }
        ImageEntity image = coverImageMap.get(coverImageId);
        if (image == null) {
            return null;
        }
        CampusEntity campus = campusMap.get(entity.getCampusId());
        if (campus == null) {
            return null;
        }
        UniversityEntity university = universityMap.get(campus.getUniversityId());
        if (university == null) {
            return null;
        }
        return imageUrlBuilder.buildUrl(
                university.getId(), campus.getId(), entity.getId(),
                image.getId(), image.getFileExt());
    }
}
