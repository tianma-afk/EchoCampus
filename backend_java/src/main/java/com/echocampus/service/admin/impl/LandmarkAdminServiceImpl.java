package com.echocampus.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.dto.FloorCreateDTO;
import com.echocampus.dto.LandmarkCreateRequest;
import com.echocampus.dto.LandmarkUpdateRequest;
import com.echocampus.entity.CampusEntity;
import com.echocampus.entity.CategoryEntity;
import com.echocampus.entity.FloorEntity;
import com.echocampus.entity.ImageEntity;
import com.echocampus.entity.LandmarkEntity;
import com.echocampus.entity.UniversityEntity;
import com.echocampus.mapper.CampusMapper;
import com.echocampus.mapper.CategoryMapper;
import com.echocampus.mapper.FloorMapper;
import com.echocampus.mapper.ImageMapper;
import com.echocampus.mapper.LandmarkMapper;
import com.echocampus.mapper.UniversityMapper;
import com.echocampus.service.admin.LandmarkAdminService;
import com.echocampus.utils.MinioUtil;
import com.echocampus.vo.FloorVO;
import com.echocampus.vo.LandmarkAdminVO;
import com.echocampus.vo.LandmarkDetailVO;
import com.echocampus.vo.LandmarkImageVO;
import io.minio.http.Method;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class LandmarkAdminServiceImpl implements LandmarkAdminService {
    private final LandmarkMapper landmarkMapper;
    private final FloorMapper floorMapper;
    private final ImageMapper imageMapper;
    private final CategoryMapper categoryMapper;
    private final CampusMapper campusMapper;
    private final UniversityMapper universityMapper;
    private final MinioUtil minioUtil;
    private final String bucket;

    public LandmarkAdminServiceImpl(LandmarkMapper landmarkMapper, FloorMapper floorMapper,
                                     ImageMapper imageMapper, CategoryMapper categoryMapper,
                                     CampusMapper campusMapper, UniversityMapper universityMapper,
                                     MinioUtil minioUtil, com.echocampus.config.MinioConfig minioConfig) {
        this.landmarkMapper = landmarkMapper;
        this.floorMapper = floorMapper;
        this.imageMapper = imageMapper;
        this.categoryMapper = categoryMapper;
        this.campusMapper = campusMapper;
        this.universityMapper = universityMapper;
        this.minioUtil = minioUtil;
        this.bucket = minioConfig.getBucket();
    }

    @Override
    public UUID createLandmark(LandmarkCreateRequest request) {
        LandmarkEntity entity = new LandmarkEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(request.getName());
        entity.setRating(request.getRating());
        entity.setCheckInCount(request.getCheckInCount());
        entity.setOpenTime(request.getOpenTime());
        entity.setCategoryId(request.getCategoryId());
        entity.setTags(request.getTags());
        entity.setBuildYear(request.getBuildYear());
        entity.setOpenTimeDetail(request.getOpenTimeDetail());
        entity.setFloors(request.getFloors());
        entity.setLocation(request.getLocation());
        entity.setDescription(request.getDescription());
        entity.setCampusId(request.getCampusId());
        entity.setTotalFloors(request.getTotalFloors());
        entity.setRecommendRate(request.getRecommendRate());
        landmarkMapper.insert(entity);

        List<FloorCreateDTO> floorList = request.getFloorList();
        if (floorList != null && !floorList.isEmpty()) {
            for (FloorCreateDTO dto : floorList) {
                FloorEntity floor = new FloorEntity();
                floor.setId(UUID.randomUUID());
                floor.setLandmarkId(entity.getId());
                floor.setFloorNumber(dto.getFloorNumber());
                floor.setFloorName(dto.getFloorName());
                floor.setTags(dto.getTags());
                floorMapper.insert(floor);
            }
        }

        return entity.getId();
    }

    @Override
    public Page<LandmarkAdminVO> listLandmarks(int page, int pageSize, UUID categoryId, UUID campusId, String keyword) {
        Page<LandmarkEntity> entityPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<LandmarkEntity> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(LandmarkEntity::getCategoryId, categoryId);
        }
        if (campusId != null) {
            wrapper.eq(LandmarkEntity::getCampusId, campusId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(LandmarkEntity::getName, keyword);
        }
        wrapper.orderByAsc(LandmarkEntity::getName);
        landmarkMapper.selectPage(entityPage, wrapper);

        List<LandmarkEntity> entities = entityPage.getRecords();

        // batch resolve category names
        List<UUID> categoryIds = entities.stream().map(LandmarkEntity::getCategoryId).distinct().toList();
        Map<UUID, String> categoryNameMap = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, CategoryEntity::getName));

        // batch resolve campus names
        List<UUID> campusIds = entities.stream().map(LandmarkEntity::getCampusId).distinct().toList();
        Map<UUID, CampusEntity> campusMap = campusMapper.selectBatchIds(campusIds).stream()
                .collect(Collectors.toMap(CampusEntity::getId, c -> c));

        // batch resolve university names via campus
        List<UUID> universityIds = campusMap.values().stream()
                .map(CampusEntity::getUniversityId).distinct().toList();
        Map<UUID, String> universityNameMap = universityMapper.selectBatchIds(universityIds).stream()
                .collect(Collectors.toMap(UniversityEntity::getId, UniversityEntity::getName));

        List<LandmarkAdminVO> voList = entities.stream().map(e -> {
            String coverUrl = null;
            if (e.getCoverImageId() != null) {
                coverUrl = buildImageUrl(e, e.getCoverImageId());
            }
            String campusName = null;
            String universityName = null;
            CampusEntity campus = campusMap.get(e.getCampusId());
            if (campus != null) {
                campusName = campus.getName();
                universityName = universityNameMap.get(campus.getUniversityId());
            }
            return LandmarkAdminVO.builder()
                    .id(e.getId())
                    .name(e.getName())
                    .categoryName(categoryNameMap.getOrDefault(e.getCategoryId(), ""))
                    .categoryId(e.getCategoryId())
                    .universityName(universityName)
                    .campusName(campusName)
                    .campusId(e.getCampusId())
                    .coverImageUrl(coverUrl)
                    .rating(e.getRating())
                    .checkInCount(e.getCheckInCount())
                    .recommendRate(e.getRecommendRate())
                    .buildYear(e.getBuildYear())
                    .openTime(e.getOpenTime())
                    .build();
        }).toList();

        Page<LandmarkAdminVO> voPage = new Page<>(page, pageSize, entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public LandmarkDetailVO getLandmark(UUID id) {
        LandmarkEntity entity = landmarkMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("地标不存在");
        }

        CategoryEntity category = categoryMapper.selectById(entity.getCategoryId());
        CampusEntity campus = campusMapper.selectById(entity.getCampusId());
        String universityName = null;
        if (campus != null) {
            UniversityEntity university = universityMapper.selectById(campus.getUniversityId());
            if (university != null) {
                universityName = university.getName();
            }
        }

        List<FloorEntity> floors = floorMapper.selectList(
                new LambdaQueryWrapper<FloorEntity>().eq(FloorEntity::getLandmarkId, id));

        List<FloorVO> floorVOList = floors.stream()
                .map(f -> FloorVO.builder()
                        .id(f.getId())
                        .floorNumber(f.getFloorNumber())
                        .floorName(f.getFloorName())
                        .tags(f.getTags())
                        .build())
                .toList();

        // build image URLs for curated images
        List<String> imgUrls = null;
        if (entity.getImgs() != null && !entity.getImgs().isEmpty()) {
            imgUrls = entity.getImgs().stream()
                    .map(imgId -> buildImageUrl(entity, imgId))
                    .toList();
        }

        return LandmarkDetailVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .rating(entity.getRating())
                .checkins(entity.getCheckInCount())
                .openTime(entity.getOpenTime())
                .category(category != null ? category.getName() : null)
                .categoryId(entity.getCategoryId())
                .tags(entity.getTags())
                .imgs(imgUrls)
                .buildYear(entity.getBuildYear())
                .openTimeDetail(entity.getOpenTimeDetail())
                .floors(entity.getFloors())
                .location(entity.getLocation())
                .description(entity.getDescription())
                .campusName(campus != null ? campus.getName() : null)
                .campusId(entity.getCampusId())
                .universityName(universityName)
                .universityId(campus != null ? campus.getUniversityId() : null)
                .totalFloors(entity.getTotalFloors())
                .recommendRate(entity.getRecommendRate())
                .floorList(floorVOList)
                .build();
    }

    @Override
    public void updateLandmark(UUID id, LandmarkUpdateRequest request) {
        LandmarkEntity entity = landmarkMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("地标不存在");
        }
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getRating() != null) entity.setRating(request.getRating());
        if (request.getCheckInCount() != null) entity.setCheckInCount(request.getCheckInCount());
        if (request.getOpenTime() != null) entity.setOpenTime(request.getOpenTime());
        if (request.getCategoryId() != null) entity.setCategoryId(request.getCategoryId());
        if (request.getTags() != null) entity.setTags(request.getTags());
        if (request.getBuildYear() != null) entity.setBuildYear(request.getBuildYear());
        if (request.getOpenTimeDetail() != null) entity.setOpenTimeDetail(request.getOpenTimeDetail());
        if (request.getFloors() != null) entity.setFloors(request.getFloors());
        if (request.getLocation() != null) entity.setLocation(request.getLocation());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getCampusId() != null) entity.setCampusId(request.getCampusId());
        if (request.getTotalFloors() != null) entity.setTotalFloors(request.getTotalFloors());
        if (request.getRecommendRate() != null) entity.setRecommendRate(request.getRecommendRate());
        landmarkMapper.updateById(entity);

        if (request.getFloorList() != null) {
            floorMapper.delete(new LambdaQueryWrapper<FloorEntity>().eq(FloorEntity::getLandmarkId, id));
            for (FloorCreateDTO dto : request.getFloorList()) {
                FloorEntity floor = new FloorEntity();
                floor.setId(UUID.randomUUID());
                floor.setLandmarkId(id);
                floor.setFloorNumber(dto.getFloorNumber());
                floor.setFloorName(dto.getFloorName());
                floor.setTags(dto.getTags());
                floorMapper.insert(floor);
            }
        }
    }

    @Override
    public void deleteLandmark(UUID id) {
        LandmarkEntity entity = landmarkMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("地标不存在");
        }

        // delete images from MinIO and DB
        List<ImageEntity> images = imageMapper.selectList(
                new LambdaQueryWrapper<ImageEntity>().eq(ImageEntity::getLandmarkId, id));
        if (!images.isEmpty()) {
            CampusEntity campus = campusMapper.selectById(entity.getCampusId());
            UniversityEntity university = null;
            if (campus != null) {
                university = universityMapper.selectById(campus.getUniversityId());
            }
            for (ImageEntity image : images) {
                if (university != null && campus != null) {
                    String key = String.format("imgs/%s/%s/%s/%s.%s",
                            university.getId(), campus.getId(), id, image.getId(), image.getFileExt());
                    try {
                        minioUtil.removeObject(bucket, key);
                    } catch (Exception ignored) {
                    }
                }
            }
            imageMapper.delete(new LambdaQueryWrapper<ImageEntity>().eq(ImageEntity::getLandmarkId, id));
        }

        // delete floors
        floorMapper.delete(new LambdaQueryWrapper<FloorEntity>().eq(FloorEntity::getLandmarkId, id));

        // delete landmark
        landmarkMapper.deleteById(id);
    }

    private String buildImageUrl(LandmarkEntity landmark, UUID imageId) {
        ImageEntity image = imageMapper.selectById(imageId);
        if (image == null) return null;
        CampusEntity campus = campusMapper.selectById(landmark.getCampusId());
        if (campus == null) return null;
        UniversityEntity university = universityMapper.selectById(campus.getUniversityId());
        if (university == null) return null;
        String key = String.format("imgs/%s/%s/%s/%s.%s",
                university.getId(), campus.getId(), landmark.getId(), image.getId(), image.getFileExt());
        try {
            return minioUtil.getPresignedObjectUrl(bucket, key, 30, TimeUnit.MINUTES, Method.GET, null);
        } catch (Exception e) {
            return null;
        }
    }
}
