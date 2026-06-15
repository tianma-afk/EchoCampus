package com.echocampus.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.campus.entity.CampusEntity;
import com.echocampus.campus.mapper.CampusMapper;
import com.echocampus.category.entity.CategoryEntity;
import com.echocampus.category.mapper.CategoryMapper;
import com.echocampus.landmark.entity.ImageEntity;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.landmark.mapper.ImageMapper;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.util.ImageUrlBuilder;
import com.echocampus.university.entity.UniversityEntity;
import com.echocampus.university.mapper.UniversityMapper;
import com.echocampus.user.entity.Rating;
import com.echocampus.user.mapper.RatingMapper;
import com.echocampus.user.service.RatingService;
import com.echocampus.user.vo.RatingVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RatingServiceImpl implements RatingService {

    private final RatingMapper ratingMapper;
    private final LandmarkMapper landmarkMapper;
    private final CategoryMapper categoryMapper;
    private final ImageMapper imageMapper;
    private final CampusMapper campusMapper;
    private final UniversityMapper universityMapper;
    private final ImageUrlBuilder imageUrlBuilder;

    public RatingServiceImpl(RatingMapper ratingMapper, LandmarkMapper landmarkMapper,
                             CategoryMapper categoryMapper, ImageMapper imageMapper,
                             CampusMapper campusMapper, UniversityMapper universityMapper,
                             ImageUrlBuilder imageUrlBuilder) {
        this.ratingMapper = ratingMapper;
        this.landmarkMapper = landmarkMapper;
        this.categoryMapper = categoryMapper;
        this.imageMapper = imageMapper;
        this.campusMapper = campusMapper;
        this.universityMapper = universityMapper;
        this.imageUrlBuilder = imageUrlBuilder;
    }

    @Override
    public void rate(UUID userId, UUID landmarkId, BigDecimal rating) {
        LandmarkEntity landmark = landmarkMapper.selectById(landmarkId);
        if (landmark == null) {
            throw new BusinessException(ErrorCode.RATING_LANDMARK_NOT_FOUND);
        }

        Rating existing = ratingMapper.selectOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getLandmarkId, landmarkId));
        if (existing != null) {
            existing.setRating(rating);
            existing.setUpdatedAt(OffsetDateTime.now());
            ratingMapper.updateById(existing);
            log.info("[评分] 更新评分 -> userId={}, landmarkId={}, rating={}", userId, landmarkId, rating);
        } else {
            Rating r = new Rating();
            r.setId(UUID.randomUUID());
            r.setUserId(userId);
            r.setLandmarkId(landmarkId);
            r.setRating(rating);
            r.setCreatedAt(OffsetDateTime.now());
            r.setUpdatedAt(OffsetDateTime.now());
            ratingMapper.insert(r);
            log.info("[评分] 新增评分 -> userId={}, landmarkId={}, rating={}", userId, landmarkId, rating);
        }
    }

    @Override
    public RatingVO getUserRating(UUID userId, UUID landmarkId) {
        Rating rating = ratingMapper.selectOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getLandmarkId, landmarkId));
        if (rating == null) {
            return null;
        }
        return buildRatingVO(rating);
    }

    @Override
    public Page<RatingVO> getUserRatings(UUID userId, int page, int size) {
        Page<Rating> entityPage = new Page<>(page, size);
        ratingMapper.selectPage(entityPage, new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .orderByDesc(Rating::getUpdatedAt));

        List<Rating> records = entityPage.getRecords();
        if (records.isEmpty()) {
            Page<RatingVO> emptyPage = new Page<>(page, size, entityPage.getTotal());
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        List<UUID> landmarkIds = records.stream()
                .map(Rating::getLandmarkId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, LandmarkEntity> landmarkMap = landmarkMapper.selectBatchIds(landmarkIds).stream()
                .collect(Collectors.toMap(LandmarkEntity::getId, l -> l));

        Map<UUID, String> categoryNameMap = buildCategoryNameMap(landmarkMap);
        Map<UUID, String> coverUrlMap = buildCoverUrlMap(landmarkMap);

        List<RatingVO> voList = records.stream()
                .map(r -> {
                    LandmarkEntity lm = landmarkMap.get(r.getLandmarkId());
                    return RatingVO.builder()
                            .id(r.getId())
                            .landmarkId(r.getLandmarkId())
                            .landmarkName(lm != null ? lm.getName() : null)
                            .coverImageUrl(coverUrlMap.get(r.getLandmarkId()))
                            .landmarkRating(lm != null ? lm.getRating() : null)
                            .category(categoryNameMap.get(r.getLandmarkId()))
                            .rating(r.getRating())
                            .createdAt(r.getCreatedAt())
                            .updatedAt(r.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        Page<RatingVO> voPage = new Page<>(page, size, entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private RatingVO buildRatingVO(Rating rating) {
        LandmarkEntity lm = landmarkMapper.selectById(rating.getLandmarkId());
        if (lm == null) {
            return RatingVO.builder()
                    .id(rating.getId())
                    .landmarkId(rating.getLandmarkId())
                    .rating(rating.getRating())
                    .createdAt(rating.getCreatedAt())
                    .updatedAt(rating.getUpdatedAt())
                    .build();
        }
        String categoryName = null;
        if (lm.getCategoryId() != null) {
            CategoryEntity cat = categoryMapper.selectById(lm.getCategoryId());
            categoryName = cat != null ? cat.getName() : null;
        }
        String coverUrl = buildSingleCoverUrl(lm);
        return RatingVO.builder()
                .id(rating.getId())
                .landmarkId(rating.getLandmarkId())
                .landmarkName(lm.getName())
                .coverImageUrl(coverUrl)
                .landmarkRating(lm.getRating())
                .category(categoryName)
                .rating(rating.getRating())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }

    private Map<UUID, String> buildCategoryNameMap(Map<UUID, LandmarkEntity> landmarkMap) {
        List<UUID> categoryIds = landmarkMap.values().stream()
                .map(LandmarkEntity::getCategoryId)
                .distinct()
                .collect(Collectors.toList());
        if (categoryIds.isEmpty()) return Map.of();
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, CategoryEntity::getName));
    }

    private Map<UUID, String> buildCoverUrlMap(Map<UUID, LandmarkEntity> landmarkMap) {
        List<UUID> coverImageIds = landmarkMap.values().stream()
                .map(LandmarkEntity::getCoverImageId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (coverImageIds.isEmpty()) return Map.of();
        Map<UUID, ImageEntity> imageMap = imageMapper.selectBatchIds(coverImageIds).stream()
                .collect(Collectors.toMap(ImageEntity::getId, img -> img));

        List<UUID> campusIds = landmarkMap.values().stream()
                .map(LandmarkEntity::getCampusId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, CampusEntity> campusMap = campusMapper.selectBatchIds(campusIds).stream()
                .collect(Collectors.toMap(CampusEntity::getId, c -> c));
        List<UUID> universityIds = campusMap.values().stream()
                .map(CampusEntity::getUniversityId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, UniversityEntity> universityMap = universityMapper.selectBatchIds(universityIds).stream()
                .collect(Collectors.toMap(UniversityEntity::getId, u -> u));

        return landmarkMap.values().stream()
                .filter(lm -> lm.getCoverImageId() != null)
                .collect(Collectors.toMap(
                        LandmarkEntity::getId,
                        lm -> {
                            ImageEntity img = imageMap.get(lm.getCoverImageId());
                            if (img == null) return null;
                            CampusEntity campus = campusMap.get(lm.getCampusId());
                            if (campus == null) return null;
                            UniversityEntity uni = universityMap.get(campus.getUniversityId());
                            if (uni == null) return null;
                            return imageUrlBuilder.buildUrl(
                                    uni.getId(), campus.getId(), lm.getId(),
                                    img.getId(), img.getFileExt());
                        }));
    }

    private String buildSingleCoverUrl(LandmarkEntity lm) {
        if (lm.getCoverImageId() == null) return null;
        ImageEntity img = imageMapper.selectById(lm.getCoverImageId());
        if (img == null) return null;
        CampusEntity campus = campusMapper.selectById(lm.getCampusId());
        if (campus == null) return null;
        UniversityEntity uni = universityMapper.selectById(campus.getUniversityId());
        if (uni == null) return null;
        return imageUrlBuilder.buildUrl(
                uni.getId(), campus.getId(), lm.getId(),
                img.getId(), img.getFileExt());
    }
}
