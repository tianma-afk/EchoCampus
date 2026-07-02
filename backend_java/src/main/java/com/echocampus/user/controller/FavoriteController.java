package com.echocampus.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.context.AuthContext;
import com.echocampus.shared.dto.BatchDeleteRequest;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.vo.Result;
import com.echocampus.user.dto.FavoriteToggleRequest;
import com.echocampus.user.service.FavoriteService;
import com.echocampus.user.vo.FavoriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/user/favorites")
@RequireRole(RoleEnum.USER)
@Tag(name = "用户收藏", description = "用户收藏相关接口")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    @Operation(summary = "切换收藏", description = "收藏/取消收藏地标，返回当前收藏状态")
    public Result<Boolean> toggleFavorite(@Valid @RequestBody FavoriteToggleRequest request) {
        UUID userId = UUID.fromString(AuthContext.get().getUserId());
        boolean favorited = favoriteService.toggleFavorite(userId, request.getLandmarkId());
        String msg = favorited ? "已收藏" : "已取消收藏";
        return Result.success(ErrorCode.SUCCESS, msg, favorited);
    }

    @GetMapping
    @Operation(summary = "获取收藏列表", description = "分页获取当前用户的收藏列表")
    public Result<Page<FavoriteVO>> getFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = UUID.fromString(AuthContext.get().getUserId());
        return Result.success(favoriteService.getFavorites(userId, page, size));
    }

    @GetMapping("/{landmarkId}")
    @Operation(summary = "检查是否已收藏", description = "检查当前用户是否收藏了指定地标")
    public Result<Boolean> isFavorited(@PathVariable UUID landmarkId) {
        UUID userId = UUID.fromString(AuthContext.get().getUserId());
        return Result.success(favoriteService.isFavorited(userId, landmarkId));
    }

    @DeleteMapping
    @Operation(summary = "批量删除收藏记录")
    public Result<Void> batchDelete(@Valid @RequestBody BatchDeleteRequest request) {
        UUID userId = UUID.fromString(AuthContext.get().getUserId());
        favoriteService.batchDelete(userId, request.getIds());
        return Result.success(ErrorCode.SUCCESS, "删除成功", null);
    }
}
