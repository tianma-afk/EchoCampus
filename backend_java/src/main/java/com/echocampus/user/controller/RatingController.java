package com.echocampus.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.context.AuthContext;
import com.echocampus.shared.dto.BatchDeleteRequest;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.vo.Result;
import com.echocampus.user.dto.RatingSubmitRequest;
import com.echocampus.user.service.RatingService;
import com.echocampus.user.vo.RatingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/user/ratings")
@RequireRole(RoleEnum.USER)
@Tag(name = "用户评分", description = "用户评分相关接口")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @Operation(summary = "提交评分", description = "对地标进行评分，已评分则覆盖")
    public Result<Void> submitRating(@Valid @RequestBody RatingSubmitRequest request) {
        UUID userId = UUID.fromString(AuthContext.get().getUserId());
        ratingService.rate(userId, request.getLandmarkId(), request.getRating());
        return Result.success(ErrorCode.SUCCESS, "评分成功", null);
    }

    @GetMapping("/{landmarkId}")
    @Operation(summary = "获取用户对指定地标的评分")
    public Result<RatingVO> getUserRating(@PathVariable UUID landmarkId) {
        UUID userId = UUID.fromString(AuthContext.get().getUserId());
        RatingVO rating = ratingService.getUserRating(userId, landmarkId);
        if (rating == null) {
            return Result.success(null);
        }
        return Result.success(rating);
    }

    @GetMapping
    @Operation(summary = "获取用户评分列表", description = "分页获取当前用户的所有评分记录")
    public Result<Page<RatingVO>> getUserRatings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = UUID.fromString(AuthContext.get().getUserId());
        return Result.success(ratingService.getUserRatings(userId, page, size));
    }

    @DeleteMapping
    @Operation(summary = "批量删除评分记录")
    public Result<Void> batchDelete(@Valid @RequestBody BatchDeleteRequest request) {
        UUID userId = UUID.fromString(AuthContext.get().getUserId());
        ratingService.batchDelete(userId, request.getIds());
        return Result.success(ErrorCode.SUCCESS, "删除成功", null);
    }
}
