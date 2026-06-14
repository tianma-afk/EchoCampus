package com.echocampus.feedback.service.impl;

import com.echocampus.feedback.dto.FeedbackSubmitRequest;
import com.echocampus.feedback.entity.FeedbackEntity;
import com.echocampus.feedback.mapper.FeedbackMapper;
import com.echocampus.feedback.service.FeedbackUserService;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FeedbackUserServiceImpl implements FeedbackUserService {

    private static final String FEEDBACK_RATE_KEY_PREFIX = "feedback:rate:";
    private static final long FEEDBACK_RATE_TTL = 5;

    private final FeedbackMapper feedbackMapper;
    private final StringRedisTemplate redisTemplate;

    public FeedbackUserServiceImpl(FeedbackMapper feedbackMapper, StringRedisTemplate redisTemplate) {
        this.feedbackMapper = feedbackMapper;
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
}
