package com.echocampus.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.shared.annotation.RequireRole;
import com.echocampus.shared.context.AuthContext;
import com.echocampus.shared.enums.RoleEnum;
import com.echocampus.shared.vo.Result;
import com.echocampus.user.service.RecognitionService;
import com.echocampus.user.vo.RecognitionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/user/recognitions")
@RequireRole(RoleEnum.USER)
@Tag(name = "用户识别记录", description = "用户图像识别历史记录")
public class RecognitionController {

    private final RecognitionService recognitionService;

    public RecognitionController(RecognitionService recognitionService) {
        this.recognitionService = recognitionService;
    }

    @GetMapping
    @Operation(summary = "获取识别记录列表", description = "分页获取当前用户的图像识别历史记录")
    public Result<Page<RecognitionVO>> getRecognitionHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = UUID.fromString(AuthContext.get().getUserId());
        return Result.success(recognitionService.getRecognitionHistory(userId, page, size));
    }
}
