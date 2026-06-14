package com.echocampus.user.controller;

import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.context.AuthContext;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.vo.Result;
import com.echocampus.user.service.CheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/landmarks")
@RequireRole(RoleEnum.USER)
@Tag(name = "打卡", description = "地标打卡相关接口")
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping("/{landmarkId}/checkin")
    @Operation(summary = "打卡", description = "用户对指定地标进行打卡")
    public Result<Void> doCheckin(@PathVariable UUID landmarkId) {
        String userId = AuthContext.get().getUserId();
        log.info("[打卡] userId={}, landmarkId={}", userId, landmarkId);
        try {
            checkinService.doCheckin(UUID.fromString(userId), landmarkId);
            log.info("[打卡] 成功 -> userId={}, landmarkId={}", userId, landmarkId);
            return Result.success(ErrorCode.SUCCESS, "打卡成功", null);
        } catch (BusinessException e) {
            log.warn("[打卡] 业务异常 -> {}", e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        }
    }
}
