package com.echocampus.feedback.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.feedback.dto.FeedbackResolveRequest;
import com.echocampus.feedback.vo.FeedbackAdminVO;

import java.util.UUID;

public interface FeedbackAdminService {
    Page<FeedbackAdminVO> listFeedbacks(int page, int pageSize, String status, String feedbackType);

    FeedbackAdminVO getFeedback(UUID id);

    void resolveFeedback(UUID id, FeedbackResolveRequest request);
}
