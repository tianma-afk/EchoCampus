package com.echocampus.feedback.service;

import com.echocampus.feedback.dto.FeedbackSubmitRequest;

public interface FeedbackUserService {
    void submitFeedback(String userId, FeedbackSubmitRequest request);
}
