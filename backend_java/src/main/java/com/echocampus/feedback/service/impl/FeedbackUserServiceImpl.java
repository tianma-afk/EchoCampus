package com.echocampus.feedback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.feedback.dto.FeedbackSubmitRequest;
import com.echocampus.feedback.entity.FeedbackEntity;
import com.echocampus.feedback.mapper.FeedbackMapper;
import com.echocampus.feedback.service.FeedbackUserService;
import com.echocampus.feedback.vo.FeedbackUserVO;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class FeedbackUserServiceImpl implements FeedbackUserService {

    private static final String FEEDBACK_RATE_KEY_PREFIX = "feedback:rate:";
    private static final long FEEDBACK_RATE_TTL = 5;

    private final FeedbackMapper feedbackMapper;
    private final LandmarkMapper landmarkMapper;
    private final StringRedisTemplate redisTemplate;

    public FeedbackUserServiceImpl(FeedbackMapper feedbackMapper, LandmarkMapper landmarkMapper,
                                   StringRedisTemplate redisTemplate) {
        this.feedbackMapper = feedbackMapper;
        this.landmarkMapper = landmarkMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void submitFeedback(String userId, FeedbackSubmitRequest request) {
        String rateKey = FEEDBACK_RATE_KEY_PREFIX + userId;
        Boolean canSubmit = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", FEEDBACK_RATE_TTL, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(canSubmit)) {
            throw new BusinessException(ErrorCode.FEEDBACK_SUBMIT_TOO_FREQUENT);
        }

        FeedbackEntity entity = new FeedbackEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.fromString(userId));
        entity.setLandmarkId(request.getLandmarkId());
        entity.setFeedbackType(request.getFeedbackType());
        entity.setContent(request.getContent());
        entity.setCorrectLandmarkName(request.getCorrectLandmarkName());
        entity.setUploadUrl(request.getUploadUrl());
        entity.setStatus("PENDING");
        feedbackMapper.insert(entity);
    }

    @Override
    public Page<FeedbackUserVO> getUserFeedbacks(String userId, int page, int size) {
        Page<FeedbackEntity> entityPage = new Page<>(page, size);
        LambdaQueryWrapper<FeedbackEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedbackEntity::getUserId, UUID.fromString(userId));
        wrapper.orderByDesc(FeedbackEntity::getCreatedAt);
        feedbackMapper.selectPage(entityPage, wrapper);

        List<FeedbackEntity> entities = entityPage.getRecords();

        List<UUID> landmarkIds = entities.stream().map(FeedbackEntity::getLandmarkId)
                .filter(id -> id != null).distinct().toList();
        Map<UUID, String> landmarkNameMap = landmarkIds.isEmpty() ? new HashMap<>()
                : landmarkMapper.selectBatchIds(landmarkIds).stream()
                        .collect(Collectors.toMap(LandmarkEntity::getId, LandmarkEntity::getName));

        List<FeedbackUserVO> voList = entities.stream().map(e -> FeedbackUserVO.builder()
                .id(e.getId())
                .landmarkId(e.getLandmarkId())
                .landmarkName(landmarkNameMap.getOrDefault(e.getLandmarkId(), ""))
                .feedbackType(e.getFeedbackType())
                .content(e.getContent())
                .status(e.getStatus())
                .resolveNote(e.getResolveNote())
                .resolveTime(e.getResolveTime())
                .createdAt(e.getCreatedAt())
                .uploadUrl(e.getUploadUrl())
                .correctLandmarkName(e.getCorrectLandmarkName())
                .build()).toList();

        Page<FeedbackUserVO> voPage = new Page<>(page, size, entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public FeedbackUserVO getFeedbackDetail(String userId, String feedbackId) {
        FeedbackEntity entity = feedbackMapper.selectById(UUID.fromString(feedbackId));
        if (entity == null || !entity.getUserId().equals(UUID.fromString(userId))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "反馈不存在");
        }

        String landmarkName = "";
        if (entity.getLandmarkId() != null) {
            LandmarkEntity landmark = landmarkMapper.selectById(entity.getLandmarkId());
            if (landmark != null) {
                landmarkName = landmark.getName();
            }
        }

        return FeedbackUserVO.builder()
                .id(entity.getId())
                .landmarkId(entity.getLandmarkId())
                .landmarkName(landmarkName)
                .feedbackType(entity.getFeedbackType())
                .content(entity.getContent())
                .status(entity.getStatus())
                .resolveNote(entity.getResolveNote())
                .resolveTime(entity.getResolveTime())
                .createdAt(entity.getCreatedAt())
                .uploadUrl(entity.getUploadUrl())
                .correctLandmarkName(entity.getCorrectLandmarkName())
                .build();
    }
}
