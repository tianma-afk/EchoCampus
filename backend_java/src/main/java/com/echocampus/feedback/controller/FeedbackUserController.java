package com.echocampus.feedback.controller;

import com.echocampus.feedback.dto.FeedbackSubmitRequest;
import com.echocampus.feedback.service.FeedbackUserService;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.util.JwtUtil;
import com.echocampus.shared.vo.Result;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/user/feedbacks")
@Tag(name = "用户反馈", description = "用户提交纠正反馈")
public class FeedbackUserController {
    private final FeedbackUserService feedbackUserService;
    private final JwtUtil jwtUtil;

    public FeedbackUserController(FeedbackUserService feedbackUserService, JwtUtil jwtUtil) {
        this.feedbackUserService = feedbackUserService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/")
    @Operation(summary = "提交反馈", description = "用户提交纠正反馈")
    public Result<Void> submitFeedback(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody FeedbackSubmitRequest request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtUtil.getUserIdFromToken(token);
            log.info("提交反馈: userId={}, landmarkId={}, feedbackType={}", userId, request.getLandmarkId(), request.getFeedbackType());
            feedbackUserService.submitFeedback(userId, request);
            return Result.success(null);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("提交反馈认证失败: {}", e.getMessage());
            return Result.failure(ErrorCode.USER_NOT_LOGIN);
        } catch (Exception e) {
            log.error("提交反馈异常: {} -> {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return Result.failure(ErrorCode.SYSTEM_ERROR);
        }
    }
}
