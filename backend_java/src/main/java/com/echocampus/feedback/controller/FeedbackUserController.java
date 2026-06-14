package com.echocampus.feedback.controller;

import com.echocampus.feedback.dto.FeedbackSubmitRequest;
import com.echocampus.feedback.service.FeedbackUserService;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.context.AuthContext;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/user/feedbacks")
@RequireRole(RoleEnum.USER)
@Tag(name = "用户反馈", description = "用户提交纠正反馈")
public class FeedbackUserController {
    private final FeedbackUserService feedbackUserService;

    public FeedbackUserController(FeedbackUserService feedbackUserService) {
        this.feedbackUserService = feedbackUserService;
    }

    @PostMapping("/")
    @Operation(summary = "提交反馈", description = "用户提交纠正反馈")
    public Result<Void> submitFeedback(@Valid @RequestBody FeedbackSubmitRequest request) {
        String userId = AuthContext.get().getUserId();
        log.info("提交反馈: userId={}, landmarkId={}, feedbackType={}", userId, request.getLandmarkId(), request.getFeedbackType());
        try {
            feedbackUserService.submitFeedback(userId, request);
            return Result.success(null);
        } catch (Exception e) {
            log.error("提交反馈异常: {} -> {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return Result.failure(ErrorCode.SYSTEM_ERROR);
        }
    }
}
