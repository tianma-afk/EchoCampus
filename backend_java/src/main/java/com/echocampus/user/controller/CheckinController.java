package com.echocampus.user.controller;

import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.exception.ErrorCode;
import com.echocampus.shared.util.JwtUtil;
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
@Tag(name = "打卡", description = "地标打卡相关接口")
public class CheckinController {

    private final CheckinService checkinService;
    private final JwtUtil jwtUtil;

    public CheckinController(CheckinService checkinService, JwtUtil jwtUtil) {
        this.checkinService = checkinService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/{landmarkId}/checkin")
    @Operation(summary = "打卡", description = "用户对指定地标进行打卡")
    public Result<Void> doCheckin(@PathVariable UUID landmarkId,
                                  @RequestHeader("Authorization") String authHeader) {
        log.info("[打卡] 请求到达, landmarkId={}", landmarkId);
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtUtil.getUserIdFromToken(token);
            checkinService.doCheckin(UUID.fromString(userId), landmarkId);
            log.info("[打卡] 成功 -> userId={}, landmarkId={}", userId, landmarkId);
            return Result.success(ErrorCode.SUCCESS, "打卡成功", null);
        } catch (BusinessException e) {
            log.warn("[打卡] 业务异常 -> {}", e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("[打卡] token无效 -> {}", e.getMessage());
            return Result.failure(ErrorCode.USER_NOT_LOGIN);
        }
    }
}
