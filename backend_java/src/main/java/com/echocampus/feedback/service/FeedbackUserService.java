package com.echocampus.feedback.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.feedback.dto.FeedbackSubmitRequest;
import com.echocampus.feedback.vo.FeedbackUserVO;

public interface FeedbackUserService {
    void submitFeedback(String userId, FeedbackSubmitRequest request);

    Page<FeedbackUserVO> getUserFeedbacks(String userId, int page, int size);

    FeedbackUserVO getFeedbackDetail(String userId, String feedbackId);
}
