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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LandmarkServiceImpl implements LandmarkService {

    private static final String HOT_RANKING_KEY = "landmark:hot:ranking";
    private static final String VO_KEY_PREFIX = "landmark:vo:";
    private static final long VO_TTL = 5;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LandmarkMapper landmarkMapper;
    private final CategoryMapper categoryMapper;
    private final FloorMapper floorMapper;
    private final CampusMapper campusMapper;
    private final UniversityMapper universityMapper;
    private final ImageMapper imageMapper;
    private final ImageUrlBuilder imageUrlBuilder;
    private final StringRedisTemplate redisTemplate;

    public LandmarkServiceImpl(LandmarkMapper landmarkMapper, CategoryMapper categoryMapper,
                               FloorMapper floorMapper, CampusMapper campusMapper,
                               UniversityMapper universityMapper, ImageMapper imageMapper,
                               ImageUrlBuilder imageUrlBuilder,
                               StringRedisTemplate redisTemplate) {
        this.landmarkMapper = landmarkMapper;
        this.categoryMapper = categoryMapper;
        this.floorMapper = floorMapper;
        this.campusMapper = campusMapper;
        this.universityMapper = universityMapper;
        this.imageMapper = imageMapper;
        this.imageUrlBuilder = imageUrlBuilder;
        this.redisTemplate = redisTemplate;
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
		if ("hot".equals(sortBy) && request.getCategory() == null && StringUtils.isBlank(request.getSearchQuery())) {
            return getHotLandmarkList(request);
        }
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

    private Page<LandmarkVO> getHotLandmarkList(LandmarkListRequest request) {
        int page = request.getPage();
        int pageSize = request.getPageSize();
        long start = (long) (page - 1) * pageSize;
        long end = start + pageSize - 1;

        Set<ZSetOperations.TypedTuple<String>> typedTuples;
        long total;
        try {
            total = redisTemplate.opsForZSet().zCard(HOT_RANKING_KEY);
            typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(HOT_RANKING_KEY, start, end);
        } catch (DataAccessException e) {
            log.warn("[热门排行榜] Redis 不可用, 回退到 PostgreSQL 排序", e);
            return fallbackHotList(request);
        }

        if (typedTuples == null || typedTuples.isEmpty()) {
            initHotRankingFromDb();
            try {
                total = redisTemplate.opsForZSet().zCard(HOT_RANKING_KEY);
                typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(HOT_RANKING_KEY, start, end);
            } catch (DataAccessException e) {
                log.warn("[热门排行榜] Redis 初始化后仍不可用, 回退到 PostgreSQL", e);
                return fallbackHotList(request);
            }
            if (typedTuples == null || typedTuples.isEmpty()) {
                return fallbackHotList(request);
            }
        }

        List<UUID> orderedIds = new ArrayList<>();
        Map<String, Integer> scoreMap = new HashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            String memberId = tuple.getValue();
            if (memberId != null) {
                orderedIds.add(UUID.fromString(memberId));
                scoreMap.put(memberId, tuple.getScore().intValue());
            }
        }

        List<String> cacheKeys = orderedIds.stream()
                .map(id -> VO_KEY_PREFIX + id)
                .collect(Collectors.toList());
        List<String> cachedJsonList = redisTemplate.opsForValue().multiGet(cacheKeys);

        List<UUID> missIds = new ArrayList<>();
        Map<UUID, LandmarkVO> hitMap = new HashMap<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            String json = (cachedJsonList != null && i < cachedJsonList.size()) ? cachedJsonList.get(i) : null;
            if (json != null) {
                try {
                    LandmarkVO vo = OBJECT_MAPPER.readValue(json, LandmarkVO.class);
                    vo.setCheckins(scoreMap.get(id.toString()));
                    vo.setCoverImg(null);
                    hitMap.put(id, vo);
                } catch (Exception e) {
                    missIds.add(id);
                }
            } else {
                missIds.add(id);
            }
        }

        int hitCount = orderedIds.size() - missIds.size();
        log.info("[热门排行榜] ZSET size={}, cache hits={}, misses={}, page={}",
                orderedIds.size(), hitCount, missIds.size(), page);

        if (!missIds.isEmpty()) {
            log.info("[热门排行榜] 从 PG 加载 {} 个地标详情并回填缓存", missIds.size());
            List<LandmarkEntity> missEntities = landmarkMapper.selectBatchIds(missIds);
            List<LandmarkVO> missVos = buildHotVoList(missEntities);
            for (LandmarkVO vo : missVos) {
                vo.setCheckins(scoreMap.get(vo.getId().toString()));
                vo.setCoverImg(null);
                hitMap.put(vo.getId(), vo);
                try {
                    redisTemplate.opsForValue().set(
                            VO_KEY_PREFIX + vo.getId(),
                            OBJECT_MAPPER.writeValueAsString(vo),
                            VO_TTL, TimeUnit.MINUTES);
                } catch (Exception ignored) {
                }
            }
            log.info("[热门排行榜] 回填缓存完成, 共 {} 个地标", missVos.size());
        }

        List<LandmarkVO> voList = orderedIds.stream()
                .map(hitMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Page<LandmarkVO> voPage = new Page<>(page, pageSize, total);
        voPage.setRecords(voList);
        return voPage;
    }

    private void initHotRankingFromDb() {
        log.info("[热门排行榜] 初始化 Redis ZSET, 从 PostgreSQL 加载数据");
        try {
            var wrapper = new LambdaQueryWrapper<LandmarkEntity>()
                    .select(LandmarkEntity::getId, LandmarkEntity::getCheckInCount);
            List<LandmarkEntity> all = landmarkMapper.selectList(wrapper);
            if (all.isEmpty()) return;

            Set<ZSetOperations.TypedTuple<String>> tuples = all.stream()
                    .map(e -> ZSetOperations.TypedTuple.of(
                            e.getId().toString(),
                            e.getCheckInCount() != null ? e.getCheckInCount().doubleValue() : 0.0))
                    .collect(Collectors.toSet());
            redisTemplate.opsForZSet().add(HOT_RANKING_KEY, tuples);
            log.info("[热门排行榜] 初始化完成, 共加载 {} 个地标", all.size());
        } catch (Exception e) {
            log.error("[热门排行榜] 初始化失败", e);
        }
    }

    private Page<LandmarkVO> fallbackHotList(LandmarkListRequest request) {
        LambdaQueryWrapper<LandmarkEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(LandmarkEntity::getCheckInCount);
        Page<LandmarkEntity> page = new Page<>(request.getPage(), request.getPageSize());
        Page<LandmarkEntity> resultPage = landmarkMapper.selectPage(page, wrapper);
        return buildVoPage(resultPage.getRecords(), resultPage.getCurrent(),
                resultPage.getSize(), resultPage.getTotal());
    }

    private Page<LandmarkVO> buildVoPage(List<LandmarkEntity> entities, long page, long pageSize, long total) {
        List<UUID> categoryIds = entities.stream()
                .map(LandmarkEntity::getCategoryId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, String> categoryNameMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(CategoryEntity::getId, CategoryEntity::getName));

        List<UUID> coverImageIds = entities.stream()
                .map(LandmarkEntity::getCoverImageId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, ImageEntity> coverImageMap = coverImageIds.isEmpty() ? Map.of()
                : imageMapper.selectBatchIds(coverImageIds).stream()
                        .collect(Collectors.toMap(ImageEntity::getId, img -> img));

        List<UUID> campusIds = entities.stream()
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

        List<LandmarkVO> voList = entities.stream()
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

        Page<LandmarkVO> voPage = new Page<>(page, pageSize, total);
        voPage.setRecords(voList);
        return voPage;
    }

    private List<LandmarkVO> buildHotVoList(List<LandmarkEntity> entities) {
        List<UUID> categoryIds = entities.stream()
                .map(LandmarkEntity::getCategoryId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, String> categoryNameMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(CategoryEntity::getId, CategoryEntity::getName));

        return entities.stream()
                .map(entity -> LandmarkVO.builder()
                        .id(entity.getId())
                        .name(entity.getName())
                        .rating(entity.getRating())
                        .checkins(entity.getCheckInCount())
                        .openTime(entity.getOpenTime())
                        .category(categoryNameMap.getOrDefault(entity.getCategoryId(), "未分类"))
                        .tags(entity.getTags())
                        .latitude(entity.getLatitude())
                        .longitude(entity.getLongitude())
                        .build())
                .collect(Collectors.toList());
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
