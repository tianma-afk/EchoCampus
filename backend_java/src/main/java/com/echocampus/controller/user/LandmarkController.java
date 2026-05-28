package com.echocampus.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echocampus.dto.LandmarkDetailRequest;
import com.echocampus.dto.LandmarkListRequest;
import com.echocampus.vo.LandmarkDetailVO;
import com.echocampus.vo.LandmarkVO;
import com.echocampus.vo.Result;
import com.echocampus.service.user.LandmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/landmarks")
@Tag(name = "地标", description = "提供校园地标的增删改查功能")
public class LandmarkController {
    private final LandmarkService landmarkService;

    public LandmarkController(LandmarkService landmarkService) {
        this.landmarkService = landmarkService;
    }

    @GetMapping
    @Operation(summary = "获取地标列表", description = "支持按分类筛选、关键词搜索、排序和分页")
    public Result<Page<LandmarkVO>> getLandmarkList(@Valid @RequestBody LandmarkListRequest request) {
        return Result.success(landmarkService.getLandmarkList(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取地标详情", description = "根据地标ID获取地标详情")
    public Result<LandmarkDetailVO> getLandmarkDetail(@PathVariable UUID id) {
        LandmarkDetailRequest request = new LandmarkDetailRequest();
        request.setId(id);
        return Result.success(landmarkService.getLandmarkDetail(request));
    }
}
