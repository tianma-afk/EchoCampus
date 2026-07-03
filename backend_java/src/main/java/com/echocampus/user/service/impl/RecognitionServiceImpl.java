package com.echocampus.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.campus.entity.CampusEntity;
import com.echocampus.campus.mapper.CampusMapper;
import com.echocampus.landmark.entity.ImageEntity;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.landmark.mapper.ImageMapper;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.shared.util.ImageUrlBuilder;
import com.echocampus.university.entity.UniversityEntity;
import com.echocampus.university.mapper.UniversityMapper;
import com.echocampus.user.entity.RecognitionRecord;
import com.echocampus.user.entity.UserImage;
import com.echocampus.user.mapper.RecognitionRecordMapper;
import com.echocampus.user.mapper.UserImageMapper;
import com.echocampus.user.service.RecognitionService;
import com.echocampus.user.vo.RecognitionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RecognitionServiceImpl implements RecognitionService {

    private static final String UPLOAD_BUCKET = "campus";

    private final String apiPublicBase;

    private final RecognitionRecordMapper recognitionRecordMapper;
    private final UserImageMapper userImageMapper;
    private final LandmarkMapper landmarkMapper;
    private final ImageMapper imageMapper;
    private final CampusMapper campusMapper;
    private final UniversityMapper universityMapper;
    private final ImageUrlBuilder imageUrlBuilder;

    public RecognitionServiceImpl(@Value("${api.public-base:http://localhost:8080}") String apiPublicBase,
                                  RecognitionRecordMapper recognitionRecordMapper,
                                  UserImageMapper userImageMapper,
                                  LandmarkMapper landmarkMapper,
                                  ImageMapper imageMapper,
                                  CampusMapper campusMapper,
                                  UniversityMapper universityMapper,
                                  ImageUrlBuilder imageUrlBuilder) {
        this.apiPublicBase = apiPublicBase;
        this.recognitionRecordMapper = recognitionRecordMapper;
        this.userImageMapper = userImageMapper;
        this.landmarkMapper = landmarkMapper;
        this.imageMapper = imageMapper;
        this.campusMapper = campusMapper;
        this.universityMapper = universityMapper;
        this.imageUrlBuilder = imageUrlBuilder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(UUID userId, List<UUID> ids) {
        List<RecognitionRecord> entities = recognitionRecordMapper.selectList(new LambdaQueryWrapper<RecognitionRecord>()
                .in(RecognitionRecord::getId, ids)
                .eq(RecognitionRecord::getUserId, userId));
        if (entities.isEmpty()) return;

        List<UUID> validIds = entities.stream().map(RecognitionRecord::getId).collect(Collectors.toList());
        List<UUID> userImageIds = entities.stream()
                .map(RecognitionRecord::getImageId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        recognitionRecordMapper.delete(new LambdaQueryWrapper<RecognitionRecord>().in(RecognitionRecord::getId, validIds));
        if (!userImageIds.isEmpty()) {
            userImageMapper.delete(new LambdaQueryWrapper<UserImage>().in(UserImage::getId, userImageIds));
        }
        log.info("[识别] 批量删除 -> userId={}, count={}, images={}", userId, validIds.size(), userImageIds.size());
    }

    @Override
    public Page<RecognitionVO> getRecognitionHistory(UUID userId, int page, int size) {
        Page<RecognitionRecord> entityPage = new Page<>(page, size);
        recognitionRecordMapper.selectPage(entityPage, new LambdaQueryWrapper<RecognitionRecord>()
                .eq(RecognitionRecord::getUserId, userId)
                .orderByDesc(RecognitionRecord::getCreatedAt));

        List<RecognitionRecord> records = entityPage.getRecords();
        if (records.isEmpty()) {
            Page<RecognitionVO> emptyPage = new Page<>(page, size, entityPage.getTotal());
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        // 批量查图片
        List<UUID> imageIds = records.stream()
                .map(RecognitionRecord::getImageId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, UserImage> imageMap;
        if (imageIds.isEmpty()) {
            imageMap = new HashMap<>();
        } else {
            imageMap = userImageMapper.selectBatchIds(imageIds).stream()
                    .collect(Collectors.toMap(UserImage::getId, img -> img));
        }

        // 批量查匹配的地标
        List<UUID> landmarkIds = records.stream()
                .map(RecognitionRecord::getLandmarkId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, LandmarkEntity> landmarkMap;
        if (landmarkIds.isEmpty()) {
            landmarkMap = new HashMap<>();
        } else {
            landmarkMap = landmarkMapper.selectBatchIds(landmarkIds).stream()
                    .collect(Collectors.toMap(LandmarkEntity::getId, l -> l));
        }

        // 批量查封面图 + campus + university 用于构造封面的预签名URL
        List<UUID> coverImageIds = landmarkMap.values().stream()
                .map(LandmarkEntity::getCoverImageId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, ImageEntity> coverImageMap;
        if (coverImageIds.isEmpty()) {
            coverImageMap = new HashMap<>();
        } else {
            coverImageMap = imageMapper.selectBatchIds(coverImageIds).stream()
                    .collect(Collectors.toMap(ImageEntity::getId, img -> img));
        }

        List<UUID> campusIds = landmarkMap.values().stream()
                .map(LandmarkEntity::getCampusId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, CampusEntity> campusMap;
        if (campusIds.isEmpty()) {
            campusMap = new HashMap<>();
        } else {
            campusMap = campusMapper.selectBatchIds(campusIds).stream()
                    .collect(Collectors.toMap(CampusEntity::getId, c -> c));
        }
        List<UUID> universityIds = campusMap.values().stream()
                .map(CampusEntity::getUniversityId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, UniversityEntity> universityMap;
        if (universityIds.isEmpty()) {
            universityMap = new HashMap<>();
        } else {
            universityMap = universityMapper.selectBatchIds(universityIds).stream()
                    .collect(Collectors.toMap(UniversityEntity::getId, u -> u));
        }

        List<RecognitionVO> voList = records.stream()
                .map(r -> {
                    UserImage img = imageMap.get(r.getImageId());
                    String imgUrl = null;
                    if (img != null) {
                        imgUrl = String.format("%s/api/v1/upload/files?bucket=%s&object=%s",
                                apiPublicBase, UPLOAD_BUCKET, img.getObjectName());
                    }

                    String landmarkName = null;
                    String coverUrl = null;
                    UUID lmId = r.getLandmarkId();
                    if (lmId != null) {
                        LandmarkEntity lm = landmarkMap.get(lmId);
                        if (lm != null) {
                            landmarkName = lm.getName();
                            if (lm.getCoverImageId() != null) {
                                ImageEntity coverImg = coverImageMap.get(lm.getCoverImageId());
                                CampusEntity campus = campusMap.get(lm.getCampusId());
                                if (coverImg != null && campus != null) {
                                    UniversityEntity uni = universityMap.get(campus.getUniversityId());
                                    if (uni != null) {
                                        coverUrl = imageUrlBuilder.buildUrl(
                                                uni.getId(), campus.getId(), lm.getId(),
                                                coverImg.getId(), coverImg.getFileExt());
                                    }
                                }
                            }
                        }
                    }

                    return RecognitionVO.builder()
                            .id(r.getId())
                            .imageUrl(imgUrl)
                            .landmarkId(lmId)
                            .landmarkName(landmarkName)
                            .coverImageUrl(coverUrl)
                            .similarity(r.getSimilarity())
                            .createdAt(r.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        Page<RecognitionVO> voPage = new Page<>(page, size, entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
}
