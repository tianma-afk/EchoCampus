package com.echocampus.feedback.service.impl;

import com.echocampus.feedback.dto.FeedbackSubmitRequest;
import com.echocampus.feedback.entity.FeedbackEntity;
import com.echocampus.feedback.mapper.FeedbackMapper;
import com.echocampus.feedback.service.FeedbackUserService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FeedbackUserServiceImpl implements FeedbackUserService {
    private final FeedbackMapper feedbackMapper;

    public FeedbackUserServiceImpl(FeedbackMapper feedbackMapper) {
        this.feedbackMapper = feedbackMapper;
    }

    @Override
    public void submitFeedback(String userId, FeedbackSubmitRequest request) {
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
}
