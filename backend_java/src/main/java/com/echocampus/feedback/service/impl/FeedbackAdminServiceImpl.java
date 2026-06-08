package com.echocampus.feedback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.feedback.dto.FeedbackResolveRequest;
import com.echocampus.feedback.entity.FeedbackEntity;
import com.echocampus.feedback.mapper.FeedbackMapper;
import com.echocampus.feedback.service.FeedbackAdminService;
import com.echocampus.feedback.vo.FeedbackAdminVO;
import com.echocampus.landmark.entity.LandmarkEntity;
import com.echocampus.landmark.mapper.LandmarkMapper;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FeedbackAdminServiceImpl implements FeedbackAdminService {
    private final FeedbackMapper feedbackMapper;
    private final LandmarkMapper landmarkMapper;

    public FeedbackAdminServiceImpl(FeedbackMapper feedbackMapper, LandmarkMapper landmarkMapper) {
        this.feedbackMapper = feedbackMapper;
        this.landmarkMapper = landmarkMapper;
    }

    @Override
    public Page<FeedbackAdminVO> listFeedbacks(int page, int pageSize, String status, String feedbackType) {
        Page<FeedbackEntity> entityPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<FeedbackEntity> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(FeedbackEntity::getStatus, status);
        }
        if (feedbackType != null && !feedbackType.isBlank()) {
            wrapper.eq(FeedbackEntity::getFeedbackType, feedbackType);
        }
        wrapper.orderByDesc(FeedbackEntity::getCreatedAt);
        feedbackMapper.selectPage(entityPage, wrapper);

        List<FeedbackEntity> entities = entityPage.getRecords();

        List<UUID> landmarkIds = entities.stream().map(FeedbackEntity::getLandmarkId).distinct().toList();
        Map<UUID, String> landmarkNameMap = landmarkIds.isEmpty() ? Map.of()
                : landmarkMapper.selectBatchIds(landmarkIds).stream()
                        .collect(Collectors.toMap(LandmarkEntity::getId, LandmarkEntity::getName));

        List<FeedbackAdminVO> voList = entities.stream().map(e -> FeedbackAdminVO.builder()
                .id(e.getId())
                .landmarkId(e.getLandmarkId())
                .landmarkName(landmarkNameMap.getOrDefault(e.getLandmarkId(), ""))
                .userId(e.getUserId())
                .feedbackType(e.getFeedbackType())
                .content(e.getContent())
                .status(e.getStatus())
                .adminId(e.getAdminId())
                .resolveTime(e.getResolveTime())
                .resolveNote(e.getResolveNote())
                .createdAt(e.getCreatedAt())
                .build()).toList();

        Page<FeedbackAdminVO> voPage = new Page<>(page, pageSize, entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public FeedbackAdminVO getFeedback(UUID id) {
        FeedbackEntity entity = feedbackMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "反馈不存在");
        }

        String landmarkName = "";
        LandmarkEntity landmark = landmarkMapper.selectById(entity.getLandmarkId());
        if (landmark != null) {
            landmarkName = landmark.getName();
        }

        return FeedbackAdminVO.builder()
                .id(entity.getId())
                .landmarkId(entity.getLandmarkId())
                .landmarkName(landmarkName)
                .userId(entity.getUserId())
                .feedbackType(entity.getFeedbackType())
                .content(entity.getContent())
                .status(entity.getStatus())
                .adminId(entity.getAdminId())
                .resolveTime(entity.getResolveTime())
                .resolveNote(entity.getResolveNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Override
    public void resolveFeedback(UUID id, FeedbackResolveRequest request) {
        FeedbackEntity entity = feedbackMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "反馈不存在");
        }
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "只能处理待处理状态的反馈");
        }
        entity.setStatus(request.getStatus());
        entity.setAdminId(request.getAdminId());
        entity.setResolveTime(OffsetDateTime.now());
        entity.setResolveNote(request.getResolveNote());
        feedbackMapper.updateById(entity);
    }
}
