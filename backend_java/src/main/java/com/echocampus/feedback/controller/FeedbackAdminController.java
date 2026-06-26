package com.echocampus.feedback.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.feedback.dto.FeedbackResolveRequest;
import com.echocampus.feedback.service.FeedbackAdminService;
import com.echocampus.feedback.vo.FeedbackAdminVO;
import com.echocampus.shared.annotation.RequireRole;
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
@RequestMapping("/api/v1/admin/feedbacks")
@RequireRole(RoleEnum.ADMIN)
@Tag(name = "管理端反馈", description = "提供反馈的查看与处理功能")
public class FeedbackAdminController {
    private final FeedbackAdminService feedbackAdminService;

    public FeedbackAdminController(FeedbackAdminService feedbackAdminService) {
        this.feedbackAdminService = feedbackAdminService;
    }

    @GetMapping("/")
    @Operation(summary = "反馈列表", description = "分页获取反馈列表，可按状态和反馈类型筛选")
    public Result<Page<FeedbackAdminVO>> listFeedbacks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String feedbackType) {
        log.info("[管理端反馈列表] 请求 -> page={}, pageSize={}, status={}, feedbackType={}",
                page, pageSize, status, feedbackType);
        try {
            return Result.success(feedbackAdminService.listFeedbacks(page, pageSize, status, feedbackType));
        } catch (Exception e) {
            log.error("[管理端反馈列表] 查询异常 -> {}: {}", e.getClass().getName(), e.getMessage(), e);
            return Result.failure(ErrorCode.SYSTEM_ERROR);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取反馈详情", description = "根据ID获取单个反馈")
    public Result<FeedbackAdminVO> getFeedback(@PathVariable UUID id) {
        return Result.success(feedbackAdminService.getFeedback(id));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "处理反馈", description = "处理反馈（仅待处理状态可操作，设为已解决或已驳回）")
    public Result<Void> resolveFeedback(@PathVariable UUID id, @Valid @RequestBody FeedbackResolveRequest request) {
        feedbackAdminService.resolveFeedback(id, request);
        return Result.success(null);
    }
}
