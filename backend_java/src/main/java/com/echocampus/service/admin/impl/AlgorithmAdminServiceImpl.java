package com.echocampus.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echocampus.client.AlgorithmClient;
import com.echocampus.entity.CampusEntity;
import com.echocampus.entity.ImageEntity;
import com.echocampus.entity.LandmarkEntity;
import com.echocampus.entity.UniversityEntity;
import com.echocampus.enums.TaskEnum;
import com.echocampus.mapper.CampusMapper;
import com.echocampus.mapper.ImageMapper;
import com.echocampus.mapper.LandmarkMapper;
import com.echocampus.mapper.UniversityMapper;
import com.echocampus.service.admin.AlgorithmAdminService;
import com.echocampus.utils.CallBackUrlBuilder;
import com.echocampus.utils.ImageUrlBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AlgorithmAdminServiceImpl implements AlgorithmAdminService {

    private final AlgorithmClient algorithmClient;
    private final CallBackUrlBuilder callBackUrlBuilder;
    private final ImageUrlBuilder imageUrlBuilder;
    private final ImageMapper imageMapper;
    private final LandmarkMapper landmarkMapper;
    private final CampusMapper campusMapper;
    private final UniversityMapper universityMapper;

    public AlgorithmAdminServiceImpl(AlgorithmClient algorithmClient, CallBackUrlBuilder callBackUrlBuilder,
                                     ImageUrlBuilder imageUrlBuilder, ImageMapper imageMapper,
                                     LandmarkMapper landmarkMapper, CampusMapper campusMapper,
                                     UniversityMapper universityMapper) {
        this.algorithmClient = algorithmClient;
        this.callBackUrlBuilder = callBackUrlBuilder;
        this.imageUrlBuilder = imageUrlBuilder;
        this.imageMapper = imageMapper;
        this.landmarkMapper = landmarkMapper;
        this.campusMapper = campusMapper;
        this.universityMapper = universityMapper;
    }

    @Override
    public UUID createVectorTaskForAllImages() {
        List<ImageEntity> images = getUnvectoredImages();
        if (images.isEmpty()) {
            return null;
        }

        // 设置缓存，防止地标相同的情况下重复查询学院大学
        Map<UUID, LandmarkEntity> landmarkCache = new HashMap<>();
        Map<UUID, CampusEntity> campusCache = new HashMap<>();
        Map<UUID, UniversityEntity> universityCache = new HashMap<>();

        List<Map<UUID, String>> idsAndUrls = new ArrayList<>();
        for (ImageEntity image : images) {
            UUID landmarkId = image.getLandmarkId();
            LandmarkEntity landmark = landmarkCache.computeIfAbsent(landmarkId,
                    id -> landmarkMapper.selectById(id));

            UUID campusId = landmark.getCampusId();
            CampusEntity campus = campusCache.computeIfAbsent(campusId,
                    id -> campusMapper.selectById(id));

            UUID universityId = campus.getUniversityId();
            UniversityEntity university = universityCache.computeIfAbsent(universityId,
                    id -> universityMapper.selectById(id));

            String url = imageUrlBuilder.buildUrl(
                    university.getId(), campus.getId(), landmark.getId(),
                    image.getId(), image.getFileExt());
            idsAndUrls.add(Map.of(image.getId(), url));
        }

        UUID taskId = UUID.randomUUID();
        String callbackUrl = callBackUrlBuilder.build(taskId.toString(), TaskEnum.VECTORIZE);
        algorithmClient.submitVectoredTask(idsAndUrls, callbackUrl);

        return taskId;
    }

    private List<ImageEntity> getUnvectoredImages() {
        LambdaQueryWrapper<ImageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImageEntity::getIsVectorized, false);
        return imageMapper.selectList(wrapper);
    }
}
