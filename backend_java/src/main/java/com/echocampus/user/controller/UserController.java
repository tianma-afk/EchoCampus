package com.echocampus.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.context.AuthContext;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.vo.Result;
import com.echocampus.user.dto.UserUpdateRequest;
import com.echocampus.user.service.CheckinService;
import com.echocampus.user.service.UserService;
import com.echocampus.user.vo.CheckinVO;
import com.echocampus.user.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.echocampus.shared.exception.ErrorCode.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@RequireRole(RoleEnum.USER)
@Tag(name = "用户信息", description = "用户信息相关接口")
public class UserController {

    private final UserService userService;
    private final CheckinService checkinService;

    public UserController(UserService userService, CheckinService checkinService) {
        this.userService = userService;
        this.checkinService = checkinService;
    }

    @GetMapping("/profile")
    @Operation(summary = "获取当前用户信息")
    public Result<UserProfileVO> profile() {
        String userId = AuthContext.get().getUserId();
        log.info("[获取用户信息] userId={}", userId);
        try {
            UserProfileVO profile = userService.getProfile(userId);
            log.info("[获取用户信息] 成功 -> userId={}", userId);
            return Result.success(profile);
        } catch (BusinessException e) {
            log.warn("[获取用户信息] 业务异常 -> {}", e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "修改当前用户信息")
    public Result<UserProfileVO> updateProfile(@RequestBody UserUpdateRequest request) {
        String userId = AuthContext.get().getUserId();
        log.info("[修改用户信息] userId={}", userId);
        try {
            UserProfileVO profile = userService.updateProfile(userId, request);
            log.info("[修改用户信息] 成功 -> userId={}", userId);
            return Result.success(ErrorCode.SUCCESS, "修改成功", profile);
        } catch (BusinessException e) {
            log.warn("[修改用户信息] 业务异常 -> {}", e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        }
    }

    @GetMapping("/checkins")
    @Operation(summary = "获取当前用户打卡历史")
    public Result<Page<CheckinVO>> getCheckinHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = AuthContext.get().getUserId();
        log.info("[打卡历史] userId={}, page={}, size={}", userId, page, size);
        try {
            Page<CheckinVO> history = checkinService.getCheckinHistory(UUID.fromString(userId), page, size);
            return Result.success(history);
        } catch (BusinessException e) {
            log.warn("[打卡历史] 业务异常 -> {}", e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        }
    }
}
