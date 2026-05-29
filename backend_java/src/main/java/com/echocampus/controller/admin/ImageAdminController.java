package com.echocampus.controller.admin;

import com.echocampus.service.admin.AlgorithmAdminService;
import com.echocampus.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/images")
@Tag(name = "管理员分类", description = "提供图片算法功能")
public class ImageAdminController {

    private final AlgorithmAdminService algorithmAdminService;
    public ImageAdminController(AlgorithmAdminService algorithmAdminService) {
        this.algorithmAdminService = algorithmAdminService;
    }

    @GetMapping("/vector/all")
    @Operation(summary = "向量化所有图片", description = "返回向量化是否成功的结果")
    public Result<UUID> vectorAll() {
        UUID result = algorithmAdminService.createVectorTaskForAllImages();
        return Result.success(result);
    }
}

