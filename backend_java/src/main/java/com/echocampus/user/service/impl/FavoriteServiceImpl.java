package com.echocampus.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.echocampus.campus.entity.CampusEntity;
import com.echocampus.campus.mapper.CampusMapper;
import com.echocampus.category.entity.CategoryEntity;
import com.echocampus.category.mapper.CategoryMapper;
import com.echocampus.landmark.entity.ImageEntity;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.landmark.mapper.ImageMapper;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.shared.util.ImageUrlBuilder;
import com.echocampus.university.entity.UniversityEntity;
import com.echocampus.university.mapper.UniversityMapper;
import com.echocampus.user.entity.Favorite;
import com.echocampus.user.mapper.FavoriteMapper;
import com.echocampus.user.service.FavoriteService;
import com.echocampus.user.vo.FavoriteVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final LandmarkMapper landmarkMapper;
    private final CategoryMapper categoryMapper;
    private final ImageMapper imageMapper;
    private final CampusMapper campusMapper;
    private final UniversityMapper universityMapper;
    private final ImageUrlBuilder imageUrlBuilder;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, LandmarkMapper landmarkMapper,
                               CategoryMapper categoryMapper, ImageMapper imageMapper,
                               CampusMapper campusMapper, UniversityMapper universityMapper,
                               ImageUrlBuilder imageUrlBuilder) {
        this.favoriteMapper = favoriteMapper;
        this.landmarkMapper = landmarkMapper;
        this.categoryMapper = categoryMapper;
        this.imageMapper = imageMapper;
        this.campusMapper = campusMapper;
        this.universityMapper = universityMapper;
        this.imageUrlBuilder = imageUrlBuilder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(UUID userId, UUID landmarkId) {
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getLandmarkId, landmarkId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            landmarkMapper.update(null, new LambdaUpdateWrapper<LandmarkEntity>()
                    .eq(LandmarkEntity::getId, landmarkId)
                    .setSql("favorite_count = GREATEST(favorite_count - 1, 0)"));
            log.info("[收藏] 取消收藏 -> userId={}, landmarkId={}", userId, landmarkId);
            return false;
        }
        Favorite fav = new Favorite();
        fav.setId(UUID.randomUUID());
        fav.setUserId(userId);
        fav.setLandmarkId(landmarkId);
        fav.setCreatedAt(OffsetDateTime.now());
        favoriteMapper.insert(fav);
        landmarkMapper.update(null, new LambdaUpdateWrapper<LandmarkEntity>()
                .eq(LandmarkEntity::getId, landmarkId)
                .setSql("favorite_count = favorite_count + 1"));
        log.info("[收藏] 添加收藏 -> userId={}, landmarkId={}", userId, landmarkId);
        return true;
    }

    @Override
    public Page<FavoriteVO> getFavorites(UUID userId, int page, int size) {
        Page<Favorite> entityPage = new Page<>(page, size);
        favoriteMapper.selectPage(entityPage, new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreatedAt));

        List<Favorite> records = entityPage.getRecords();
        if (records.isEmpty()) {
            Page<FavoriteVO> emptyPage = new Page<>(page, size, entityPage.getTotal());
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        List<UUID> landmarkIds = records.stream()
                .map(Favorite::getLandmarkId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, LandmarkEntity> landmarkMap = landmarkMapper.selectBatchIds(landmarkIds).stream()
                .collect(Collectors.toMap(LandmarkEntity::getId, l -> l));

        Map<UUID, String> categoryNameMap = buildCategoryNameMap(landmarkMap);
        Map<UUID, String> coverUrlMap = buildCoverUrlMap(landmarkMap);

        List<FavoriteVO> voList = records.stream()
                .map(r -> {
                    LandmarkEntity lm = landmarkMap.get(r.getLandmarkId());
                    return FavoriteVO.builder()
                            .id(r.getId())
                            .landmarkId(r.getLandmarkId())
                            .landmarkName(lm != null ? lm.getName() : null)
                            .coverImageUrl(coverUrlMap.get(r.getLandmarkId()))
                            .landmarkRating(lm != null ? lm.getRating() : null)
                            .category(categoryNameMap.get(r.getLandmarkId()))
                            .createdAt(r.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        Page<FavoriteVO> voPage = new Page<>(page, size, entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public boolean isFavorited(UUID userId, UUID landmarkId) {
        return favoriteMapper.exists(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getLandmarkId, landmarkId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(UUID userId, List<UUID> ids) {
        List<Favorite> entities = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .in(Favorite::getId, ids)
                .eq(Favorite::getUserId, userId));
        if (entities.isEmpty()) return;

        Set<UUID> landmarkIds = entities.stream().map(Favorite::getLandmarkId).collect(Collectors.toSet());
        List<UUID> validIds = entities.stream().map(Favorite::getId).collect(Collectors.toList());
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().in(Favorite::getId, validIds));

        for (UUID landmarkId : landmarkIds) {
            landmarkMapper.update(null, new LambdaUpdateWrapper<LandmarkEntity>()
                    .eq(LandmarkEntity::getId, landmarkId)
                    .setSql("favorite_count = GREATEST(favorite_count - 1, 0)"));
        }
        log.info("[收藏] 批量删除 -> userId={}, count={}, landmarks={}", userId, validIds.size(), landmarkIds.size());
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
}
