package com.echocampus.service.user.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.dto.LandmarkDetailRequest;
import com.echocampus.dto.LandmarkListRequest;
import com.echocampus.entity.*;
import com.echocampus.enums.CategoryEnum;
import com.echocampus.mapper.*;
import com.echocampus.service.user.LandmarkService;
import com.echocampus.vo.FloorVO;
import com.echocampus.vo.LandmarkDetailVO;
import com.echocampus.vo.LandmarkVO;
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

	public LandmarkServiceImpl(LandmarkMapper landmarkMapper, CategoryMapper categoryMapper,
							   FloorMapper floorMapper, CampusMapper campusMapper,
							   UniversityMapper universityMapper) {
		this.landmarkMapper = landmarkMapper;
		this.categoryMapper = categoryMapper;
		this.floorMapper = floorMapper;
		this.campusMapper = campusMapper;
		this.universityMapper = universityMapper;
	}

	@Override
	public Page<LandmarkVO> getLandmarkList(LandmarkListRequest request) {
		LambdaQueryWrapper<LandmarkEntity> wrapper = new LambdaQueryWrapper<>();

		// 按分类筛选
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

		// 按关键词模糊搜索名称
		if (StringUtils.isNotBlank(request.getSearchQuery())) {
			wrapper.like(LandmarkEntity::getName, request.getSearchQuery());
		}

		// 排序
		String sortBy = request.getSortBy();
		if ("rate".equals(sortBy)) {
			wrapper.orderByDesc(LandmarkEntity::getRating);
		} else if ("hot".equals(sortBy)) {
			wrapper.orderByDesc(LandmarkEntity::getCheckInCount);
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

		List<LandmarkVO> voList = resultPage.getRecords().stream()
				.map(entity -> LandmarkVO.builder()
						.id(entity.getId())
						.name(entity.getName())
						.rating(entity.getRating())
						.checkins(entity.getCheckInCount())
						.openTime(entity.getOpenTime())
						.category(categoryNameMap.get(entity.getCategoryId()))
						.tags(entity.getTags())
						.coverImg(entity.getCoverImg())
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

		// 查询分类名称
		CategoryEntity category = categoryMapper.selectById(landmark.getCategoryId());
		String categoryName = category != null ? category.getName() : null;

		// 查询校区名称和大学名称
		CampusEntity campus = campusMapper.selectById(landmark.getCampusId());
		String campusName = campus != null ? campus.getName() : null;
		String universityName = null;
		if (campus != null) {
			UniversityEntity university = universityMapper.selectById(campus.getUniversityId());
			universityName = university != null ? university.getName() : null;
		}

		// 查询楼层列表
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
				.imgs(landmark.getImgs())
				.buildYear(landmark.getBuildYear())
				.openTimeDetail(landmark.getOpenTimeDetail())
				.floors(landmark.getFloors())
				.location(landmark.getLocation())
				.description(landmark.getDescription())
				.campusName(campusName)
				.universityName(universityName)
				.totalFloors(landmark.getTotalFloors())
				.recommendRate(landmark.getRecommendRate())
				.floorList(floorList)
				.build();
	}
}
