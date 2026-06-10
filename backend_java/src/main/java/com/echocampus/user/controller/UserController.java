package com.echocampus.user.controller;

import com.echocampus.shared.exception.BusinessException;
import com.echocampus.shared.util.JwtUtil;
import com.echocampus.shared.vo.Result;
import com.echocampus.user.service.UserService;
import com.echocampus.user.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import static com.echocampus.shared.exception.ErrorCode.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "用户信息", description = "获取当前用户信息")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/profile")
    @Operation(summary = "获取当前用户信息")
    public Result<UserProfileVO> profile(@RequestHeader("Authorization") String authHeader) {
        log.info("[获取用户信息] 请求到达");
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtUtil.getUserIdFromToken(token);
            UserProfileVO profile = userService.getProfile(userId);
            log.info("[获取用户信息] 成功 -> userId={}", userId);
            return Result.success(profile);
        } catch (BusinessException e) {
            log.warn("[获取用户信息] 业务异常 -> {}", e.getMessage());
            return Result.failure(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("[获取用户信息] token无效 -> {}", e.getMessage());
            return Result.failure(USER_NOT_LOGIN);
        }
    }
}
