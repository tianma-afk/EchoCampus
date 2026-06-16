package com.echocampus.feedback.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.feedback.dto.FeedbackSubmitRequest;
import com.echocampus.feedback.service.FeedbackUserService;
import com.echocampus.feedback.vo.FeedbackUserVO;
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

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/user/feedbacks")
@RequireRole(RoleEnum.USER)
@Tag(name = "用户反馈", description = "用户提交与查看纠正反馈")
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

    @GetMapping("/")
    @Operation(summary = "我的反馈列表", description = "分页获取当前用户的反馈列表")
    public Result<Page<FeedbackUserVO>> listMyFeedbacks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = AuthContext.get().getUserId();
        log.info("获取反馈列表: userId={}, page={}, size={}", userId, page, size);
        return Result.success(feedbackUserService.getUserFeedbacks(userId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "反馈详情", description = "获取当前用户某条反馈的详情")
    public Result<FeedbackUserVO> getFeedbackDetail(@PathVariable UUID id) {
        String userId = AuthContext.get().getUserId();
        log.info("获取反馈详情: userId={}, feedbackId={}", userId, id);
        return Result.success(feedbackUserService.getFeedbackDetail(userId, id.toString()));
    }
}
